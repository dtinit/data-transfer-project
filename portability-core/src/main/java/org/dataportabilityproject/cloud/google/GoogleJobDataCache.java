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
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.PathElement;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.dataportabilityproject.cloud.interfaces.JobDataCache;

/**
 * A {@link JobDataCache} implementation based on Google Cloud Platform's DataStore.
 */
public final class GoogleJobDataCache implements JobDataCache {
  static final String JOB_KIND = "job";
  private static final String SERVICE_KIND = "service";
  static final String USER_KEY_KIND = "user-key";
  private static final int EXPIRE_TIME_MINUTES = 10;

  private static final String JOB_CREATION_FIELD = "created";

  private static final String DATA_FIELD = "data";

  private final ImmutableList<PathElement> ancestors;

  private final Datastore datastore;
  private final LoadingCache<String, Blob> loadingCache = CacheBuilder.newBuilder()
      .expireAfterAccess(EXPIRE_TIME_MINUTES, TimeUnit.MINUTES)
      .build(new CacheLoader<String, Blob>() {
        @Override
        public Blob load(String key) throws Exception {
          Entity entity = datastore.get(getKey(key));
          return entity.getBlob(DATA_FIELD);
        }
      });

  public GoogleJobDataCache(Datastore datastore, String jobId, String service) {
    this.datastore = datastore;
    this.ancestors = ImmutableList.of(
        PathElement.of(JOB_KIND, jobId),
        PathElement.of(SERVICE_KIND, service));
  }

  @Override
  public boolean hasKey(String key) throws IOException {
    return null != datastore.get(getKey(key));
  }

  @Override
  public <T extends Serializable> T getData(String key, Class<T> clazz) throws IOException {
    Blob blob;
    try {
      blob = loadingCache.get(key);
    } catch (ExecutionException e) {
      throw new IOException(e);
    }
    ObjectInputStream in = new ObjectInputStream(blob.asInputStream());
    try {
      return (T) in.readObject();
    } catch (ClassNotFoundException e) {
      throw new IllegalStateException("problem deserilizing object", e);
    }
  }

  @Override
  public <T extends Serializable> void store(String key, T data) throws IOException {
    if (hasKey(key)) {
      throw new IllegalArgumentException("Can't restore key: " + key);
    }
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    try (ObjectOutputStream out = new ObjectOutputStream(bos)) {
      out.writeObject(data);
    }
    datastore.put(Entity.newBuilder(getKey(key))
        .set(DATA_FIELD, Blob.copyFrom(bos.toByteArray()))
        .set(JOB_CREATION_FIELD, Timestamp.now())
        .build());
  }

  private Key getKey(String key) {
    return datastore.newKeyFactory()
        .setKind(USER_KEY_KIND)
        .addAncestors(ancestors)
        .newKey(key);
  }
}
