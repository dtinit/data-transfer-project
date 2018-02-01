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
import com.google.cloud.datastore.StructuredQuery.OrderBy;
import com.google.cloud.datastore.StructuredQuery.PropertyFilter;
import com.google.cloud.datastore.TimestampValue;
import com.google.cloud.datastore.Transaction;
import com.google.common.collect.ImmutableMap;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Map;
import java.util.Map.Entry;
import org.dataportabilityproject.cloud.interfaces.PersistentKeyValueStore;
import org.dataportabilityproject.job.JobDao.JobState;
import org.dataportabilityproject.job.PortabilityJob;
import org.dataportabilityproject.job.PortabilityJobConverter;

/**
 * A {@link PersistentKeyValueStore} implementation based on Google Cloud Platform's DataStore.
 */
public final class GooglePersistentKeyValueStore implements PersistentKeyValueStore {
  private static final String KIND = "persistentKey";
  private static final String CREATED_FIELD = "created";

  private final Datastore datastore;

  public GooglePersistentKeyValueStore(Datastore datastore) {
    this.datastore = datastore;
  }

  /**
   * Puts {@code jobId} in the database and verifies it doesn't already exist. To update the value
   * for an already existing {@code jobId}, use atomicUpdate instead.
   *
   * @throws IOException if an entry already exists,
   */
  @Override
  public void put(String jobId, PortabilityJob job) throws IOException {
    Transaction transaction = datastore.newTransaction();
    Entity shouldNotExist = transaction.get(getKey(jobId));
    if (shouldNotExist != null) {
      transaction.rollback();
      throw new IOException("Entity already exists for jobID " + jobId + ": " + shouldNotExist);
    }
    Entity entity = createEntity(jobId, job.asMap());
    try {
      transaction.put(entity);
    } catch (DatastoreException e) {
      transaction.rollback();
      throw new IOException(
          "Could not update entry for jobID " + jobId + " to entity " + entity, e);
    }
    transaction.commit();
  }

  @Override
  public PortabilityJob get(String jobId) {
    Entity entity = datastore.get(getKey(jobId));
    if (entity == null) {
      return null;
    }
    return PortabilityJob.mapToJob(getProperties(entity));
  }

  @Override
  public String getFirst(JobState jobState) {
    Query<Entity> query = Query.newEntityQueryBuilder()
        .setKind(KIND)
        .setFilter(PropertyFilter.eq(PortabilityJobConverter.JOB_STATE, jobState.name()))
        .setOrderBy(OrderBy.asc("created"))
        .setLimit(1)
        .build();
    QueryResults<Entity> results = datastore.run(query);
    if (!results.hasNext()) {
      return null;
    }
    Entity entity = results.next();
    return (String) entity.getValue(PortabilityJobConverter.ID_DATA_KEY).get();
  }

  @Override
  public void delete(String jobId) throws IOException {
    try {
      datastore.delete(getKey(jobId));
    } catch (DatastoreException e) {
      throw new IOException("Could not delete job " + jobId, e);
    }
  }

  @Override
  public void atomicUpdate(String jobId, JobState previousState, PortabilityJob portabilityJob)
      throws IOException {
    Transaction transaction = datastore.newTransaction();
    Key key = getKey(jobId);

    try {
      Entity previousEntity = transaction.get(key);
      if (previousEntity == null) {
        transaction.rollback();
        throw new IOException("Could not find record for jobId " + jobId);
      }
      JobState actualState = getJobState(previousEntity);
      if (previousState != null && actualState != previousState) {
        throw new IOException("Job " + jobId + " existed in an unexpected state. "
            + "Expected: " + previousState + " but was: " + actualState);
      }

      Entity newEntity = createEntity(key, portabilityJob.asMap());
      transaction.put(newEntity);
      transaction.commit();
    } catch (Throwable t) {
      transaction.rollback();
      throw new IOException("Failed atomic update of job " + jobId + " (was state "
          + previousState + ").", t);
    }
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

  private Entity createEntity(String jobId, Map<String, Object> data) throws IOException {
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

  private Key getKey(String jobId) {
    return datastore.newKeyFactory().setKind(KIND).newKey(jobId);
  }

  private JobState getJobState(Entity entity) {
    return JobState.valueOf(entity.getString(PortabilityJobConverter.JOB_STATE));
  }
}
