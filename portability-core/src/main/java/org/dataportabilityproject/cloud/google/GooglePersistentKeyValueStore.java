package org.dataportabilityproject.cloud.google;


import com.google.cloud.Timestamp;
import com.google.cloud.datastore.Blob;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.common.collect.ImmutableMap;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Map;
import java.util.Map.Entry;
import org.dataportabilityproject.cloud.interfaces.PersistentKeyValueStore;

final class GooglePersistentKeyValueStore implements PersistentKeyValueStore {
  private static final String KIND = "persistantKey";
  private static final String CREATED_FIELD = "created";

  private final Datastore datastore;

  GooglePersistentKeyValueStore(Datastore datastore) {
    this.datastore = datastore;
  }

  @Override
  public void put(String key, Map<String, Object> data) throws IOException {
    Entity.Builder builder = Entity.newBuilder(getKey(key))
        .set(CREATED_FIELD, Timestamp.now());

    for (Entry<String, Object> entry : data.entrySet()) {
      if (entry.getValue() instanceof String) {
        builder.set(entry.getKey(), (String) entry.getValue());
      } else if (entry.getValue() instanceof Integer) {
        builder.set(entry.getKey(), (Integer) entry.getValue());
      } else if (entry.getValue() instanceof Double) {
        builder.set(entry.getKey(), (Double) entry.getValue());
      } else if (entry.getValue() instanceof Boolean) {
        builder.set(entry.getKey(), (Boolean) entry.getValue());
      } else {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bos)) {
          out.writeObject(data);
        }
        builder.set(entry.getKey(), Blob.copyFrom(bos.toByteArray()));
      }
    }

    datastore.put(builder.build());
  }

  @Override
  public Map<String, Object> get(String key) {
    ImmutableMap.Builder<String, Object> builder = new ImmutableMap.Builder<>();
    Entity entity = datastore.get(getKey(key));
    for (String property : entity.getNames()) {
      builder.put(property, entity.getValue(property));
    }

    return builder.build();
  }

  @Override
  public void delete(String key) {
    datastore.delete(getKey(key));
  }

  private Key getKey(String key) {
    return datastore.newKeyFactory().setKind(KIND).newKey(key);
  }
}
