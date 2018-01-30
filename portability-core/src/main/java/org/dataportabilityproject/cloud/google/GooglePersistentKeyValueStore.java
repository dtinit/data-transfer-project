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
import com.google.cloud.datastore.DoubleValue;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.LongValue;
import com.google.cloud.datastore.Query;
import com.google.cloud.datastore.QueryResults;
import com.google.cloud.datastore.StringValue;
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
import org.dataportabilityproject.job.JobDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link PersistentKeyValueStore} implementation based on Google Cloud Platform's DataStore.
 */
public final class GooglePersistentKeyValueStore implements PersistentKeyValueStore {
  private static final Logger logger = LoggerFactory.getLogger(GooglePersistentKeyValueStore.class);

  private static final String KIND = "persistentKey";
  private static final String CREATED_FIELD = "created";

  private final Datastore datastore;

  public GooglePersistentKeyValueStore(Datastore datastore) {
    this.datastore = datastore;
  }

  @Override
  public void put(String key, Map<String, Object> data) throws IOException {
    Entity entity = createEntity(key, data);
    datastore.put(entity);
  }

  @Override
  public Map<String, Object> get(String key) {
    Entity entity = datastore.get(getKey(key));
    return getProperties(entity);
  }

  @Override
  public String getFirst(String prefix) {
    Query<Key> query = Query.newKeyQueryBuilder().setKind(KIND).build();
    QueryResults<Key> results = datastore.run(query);
    while(results.hasNext()) {
      String key = results.next().getName();
      if(key.startsWith(prefix)) {
        return key;
      }
    }
    return null;
  }

  @Override
  public void delete(String key) {
    datastore.delete(getKey(key));
  }

  @Override
  public boolean atomicUpdate(String previousKeyStr, String newKeyStr, Map<String, Object> data) {
    Transaction transaction = datastore.newTransaction();

    try {
      Key previousKey = getKey(previousKeyStr);
      Entity previousEntity = transaction.get(previousKey);
      if (previousEntity == null) {
        logger.debug("Could not find previous key {}", previousKeyStr);
        transaction.rollback();
        return false;
      }

      Key newKey = getKey(newKeyStr);
      Entity newEntity = transaction.get(newKey);
      if (newEntity != null) {
        logger.debug("Updated key already exists: {}", newKeyStr);
        transaction.rollback();
        return false;
      }

      newEntity = createEntity(newKey, data);
      transaction.put(newEntity);
      transaction.delete(previousKey);
      transaction.commit();
      return true;
    } catch (Throwable t) {
      logger.debug("Failed atomic update of {} to {}. Exception was: {}",
          previousKeyStr, newKeyStr, t);
      transaction.rollback();
      return false;
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

  private Entity createEntity(String key, Map<String, Object> data) throws IOException {
    return createEntity(getKey(key), data);
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

  private Key getKey(String key) {
    return datastore.newKeyFactory().setKind(KIND).newKey(key);
  }
}
