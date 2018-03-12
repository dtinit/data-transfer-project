/*
 * Copyright 2018 The Data Transfer Project Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.dataportabilityproject.worker;

import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.common.collect.ImmutableClassToInstanceMap;
import org.dataportabilityproject.api.launcher.ExtensionContext;
import org.dataportabilityproject.api.launcher.Logger;

/**
 * {@link ExtensionContext} used by the worker.
 */
final class WorkerExtensionContext implements ExtensionContext {
  private static final ImmutableClassToInstanceMap<Object> SERVICE_MAP =
      new ImmutableClassToInstanceMap.Builder<>()
          .put(HttpTransport.class, new NetHttpTransport())
          .put(JsonFactory.class, new JacksonFactory())
          .build();

  @Override
  public Logger getLogger() {
    return null;
  }

  @Override
  public <T> T getService(Class<T> type) {
    // returns null if no instance
    return SERVICE_MAP.getInstance(type);
  }

  @Override
  public <T> T getConfiguration(String key, T defaultValue) {
    // TODO parse flags. Returning hardcoded local+GOOGLE config now for testing.
    if (key.equals("cloud")) {
      return (T) "GOOGLE";
    }
    return defaultValue;
  }
}
