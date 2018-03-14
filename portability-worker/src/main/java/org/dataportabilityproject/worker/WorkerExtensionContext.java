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
import com.google.common.collect.ImmutableClassToInstanceMap;
import com.google.common.collect.ImmutableList;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.dataportabilityproject.api.launcher.ExtensionContext;
import org.dataportabilityproject.api.launcher.Logger;
import org.dataportabilityproject.api.launcher.TypeManager;
import org.dataportabilityproject.launcher.impl.TypeManagerImpl;
import org.dataportabilityproject.security.ConfigUtils;

/**
 * {@link ExtensionContext} used by the worker.
 */
final class WorkerExtensionContext implements ExtensionContext {
  private static final String WORKER_SETTINGS_PATH = "config/worker.yaml";
  private static final String ENV_WORKER_SETTINGS_PATH = "config/env/worker.yaml";
  private static final String COMMON_SETTINGS_PATH = "config/common.yaml";
  private static final String ENV_COMMON_SETTINGS_PATH = "config/env/common.yaml";

  private static final ImmutableClassToInstanceMap<Object> SERVICE_MAP =
      new ImmutableClassToInstanceMap.Builder<>()
          .put(HttpTransport.class, new NetHttpTransport())
          .build();

  private final Map<String, Object> config;
  private final TypeManager typeManager;

  WorkerExtensionContext() {
    // TODO init with types
    this.typeManager = new TypeManagerImpl();
    try {
      // TODO: read settings from files in jar once they are built in
      ImmutableList<String> settingsFiles = ImmutableList.<String>builder()
          .add(WORKER_SETTINGS_PATH)
          .add(ENV_WORKER_SETTINGS_PATH)
          .add(COMMON_SETTINGS_PATH)
          .add(ENV_COMMON_SETTINGS_PATH)
          .build();
      // InputStream in = ConfigUtils.getSettingsCombinedInputStream(settingsFiles);

      String tempSettings = "environment: LOCAL\ncloud: GOOGLE";
      InputStream in = new ByteArrayInputStream(tempSettings.getBytes(StandardCharsets.UTF_8));
      config = ConfigUtils.parse(in);
    } catch (IOException e) {
      throw new IllegalArgumentException("Problem parsing cloud extension settings", e);
    }
  }

  @Override
  public Logger getLogger() {
    return null;
  }

  @Override
  public TypeManager getTypeManager() {
    return typeManager;
  }

  @Override
  public <T> T getService(Class<T> type) {
    // returns null if no instance
    return SERVICE_MAP.getInstance(type);
  }

  @Override
  public <T> T getConfiguration(String key, T defaultValue) {
    if (config.containsKey(key)) {
      return (T) config.get(key);
    }
    return defaultValue;
  }
}
