/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.dataportabilityproject.cloud.google;

import com.google.cloud.Timestamp;
import com.google.cloud.datastore.Blob;
import com.google.cloud.datastore.BooleanValue;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreException;
import com.google.cloud.datastore.DoubleValue;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.LongValue;
import com.google.cloud.datastore.Query;
import com.google.cloud.datastore.QueryResults;
import com.google.cloud.datastore.StringValue;
import com.google.cloud.datastore.StructuredQuery.PropertyFilter;
import com.google.cloud.datastore.TimestampValue;
import com.google.cloud.datastore.Transaction;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import org.dataportabilityproject.spi.cloud.storage.JobStore;
import org.dataportabilityproject.spi.cloud.types.JobAuthorization;
import org.dataportabilityproject.spi.cloud.types.PortabilityJob;

/**
 * A {@link JobStore} implementation based on Google Cloud Platform's Datastore.
 */
public final class GoogleJobStore implements JobStore {
  private static final String KIND = "persistentKey";
  private static final String CREATED_FIELD = "created";

  private final Datastore datastore;

  public GoogleJobStore(Datastore datastore) {
    this.datastore = datastore;
  }

  /**
   * Inserts a new {@link PortabilityJob} keyed by its job ID in Datastore.
   *
   * <p>To update an existing {@link PortabilityJob} instead, use {@link #update}.
   *
   * @throws IOException if a job already exists for {@code job}'s ID, or if there was a different
   * problem inserting the job.
   */
  @Override
  public void createJob(UUID jobId, PortabilityJob job) throws IOException {
    Preconditions.checkNotNull(jobId);
    Transaction transaction = datastore.newTransaction();
    Entity shouldNotExist = transaction.get(getKey(jobId));
    if (shouldNotExist != null) {
      transaction.rollback();
      throw new IOException("Record already exists for jobID: " + jobId + ". Record: "
          + shouldNotExist);
    }
    Entity entity = createEntity(jobId, job.toMap());
    try {
      transaction.put(entity);
    } catch (DatastoreException e) {
      transaction.rollback();
      throw new IOException(
          "Could not create initial record for jobID: " + jobId + ". Record: " + entity, e);
    }
    transaction.commit();
  }

  /**
   * Atomically updates the {@link PortabilityJob} keyed by {@code jobId} to {@code job},
   * in Datastore using a {@link Transaction}.
   *
   * TODO(rtannenbaum): Consider validating authorization state was the previous one, when updating
   * authorization state within this transaction. Previous API allowed for passing in of a previous
   * expected state, but we shouldn't need to pass that in, given the context of the new state we
   * should know what comes before it.
   */
  @Override
  public void updateJob(UUID jobId, PortabilityJob job) throws IOException {
    Preconditions.checkNotNull(jobId);
    Transaction transaction = datastore.newTransaction();
    Key key = getKey(jobId);

    try {
      Entity previousEntity = transaction.get(key);
      if (previousEntity == null) {
        transaction.rollback();
        throw new IOException("Could not find record for jobId: " + jobId);
      }

      Entity newEntity = createEntity(key, job.toMap());
      transaction.put(newEntity);
      transaction.commit();
    } catch (Throwable t) {
      transaction.rollback();
      throw new IOException("Failed atomic update of jobId: " + jobId, t);
    }
  }

  /**
   * Removes the {@link PortabilityJob} keyed by {@code jobId} in Datastore.
   *
   * @throws IOException if the job doesn't exist, or there was a different problem deleting it.
   */
  @Override
  public void remove(UUID jobId) throws IOException {
    try {
      datastore.delete(getKey(jobId));
    } catch (DatastoreException e) {
      throw new IOException("Could not remove jobId: " + jobId, e);
    }
  }

  /**
   * Returns the job for the id or null if not found.
   *
   * @param jobId the job id
   */
  @Override
  public PortabilityJob findJob(UUID jobId) {
    Entity entity = datastore.get(getKey(jobId));
    if (entity == null) {
      return null;
    }
    return PortabilityJob.fromMap(getProperties(entity));
  }

  /**
   * Finds the ID of the first {@link PortabilityJob} in state {@code jobState} in Datastore,
   * or null if none found.
   *
   * TODO(rtannenbaum): Order by creation time so we can process jobs in a FIFO manner. Trying to
   * OrderBy.asc("created") currently fails because we don't yet have an index set up.
   */
  @Override
  public UUID findFirst(JobAuthorization.State jobState) {
    Query<Key> query = Query.newKeyQueryBuilder()
        .setKind(KIND)
        .setFilter(PropertyFilter.eq(PortabilityJob.AUTHORIZATION_STATE, jobState.name()))
        //.setOrderBy(OrderBy.asc("created"))
        .setLimit(1)
        .build();
    QueryResults<Key> results = datastore.run(query);
    if (!results.hasNext()) {
      return null;
    }
    Key key = results.next();
    return UUID.fromString(key.getName());
  }

  private Entity createEntity(Key key, Map<String, Object> data) throws IOException {
    Entity.Builder builder = Entity.newBuilder(key).set(CREATED_FIELD, Timestamp.now());

    for (Entry<String, Object> entry : data.entrySet()) {
      if (entry.getValue() instanceof String) {
        builder.set(entry.getKey(), (String) entry.getValue()); // StringValue
      } else if (entry.getValue() instanceof Integer) {
        builder.set(entry.getKey(), (Integer) entry.getValue()); // LongValue
      } else if (entry.getValue() instanceof Double) {
        builder.set(entry.getKey(), (Double) entry.getValue()); // DoubleValue
      } else if (entry.getValue() instanceof Boolean) {
        builder.set(entry.getKey(), (Boolean) entry.getValue()); // BooleanValue
      } else if (entry.getValue() instanceof Timestamp) {
        builder.set(entry.getKey(), (Timestamp) entry.getValue()); // TimestampValue
      } else {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bos)) {
          out.writeObject(entry.getValue());
        }
        builder.set(entry.getKey(), Blob.copyFrom(bos.toByteArray())); // BlobValue
      }
    }
    return builder.build();
  }

  private Entity createEntity(UUID jobId, Map<String, Object> data) throws IOException {
    return createEntity(getKey(jobId), data);
  }

  private static Map<String, Object> getProperties(Entity entity) {
    if (entity == null) return null;
    ImmutableMap.Builder<String, Object> builder = new ImmutableMap.Builder<>();
    for (String property : entity.getNames()) {
      // builder.put(property, entity.getValue(property));
      if (entity.getValue(property) instanceof StringValue) {
        builder.put(property, (String) entity.getString(property));
      } else if (entity.getValue(property) instanceof LongValue) {
        // This conversion is safe because of integer to long conversion above
        builder.put(property, new Long(entity.getLong(property)).intValue());
      } else if (entity.getValue(property) instanceof DoubleValue) {
        builder.put(property, (Double) entity.getDouble(property));
      } else if (entity.getValue(property) instanceof BooleanValue) {
        builder.put(property, (Boolean) entity.getBoolean(property));
      } else if (entity.getValue(property) instanceof TimestampValue) {
        builder.put(property, (Timestamp) entity.getTimestamp(property));
      } else {
        Blob blob = entity.getBlob(property);
        Object obj = null;
        try {
          try (ObjectInputStream in = new ObjectInputStream(blob.asInputStream())) {
            try {
              obj = in.readObject();
            } catch (ClassNotFoundException e) {
              e.printStackTrace();
            }
          }
        } catch (IOException e) {
          e.printStackTrace();
        }
        builder.put(property, obj); // BlobValue
      }
    }

    return builder.build();
  }

  private Key getKey(UUID jobId) {
    return datastore.newKeyFactory().setKind(KIND).newKey(jobId.toString());
  }
}
