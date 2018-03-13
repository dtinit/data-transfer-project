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
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.dataportabilityproject.api.launcher.ExtensionContext;
import org.dataportabilityproject.api.launcher.Logger;
import org.dataportabilityproject.api.launcher.TypeManager;
import org.dataportabilityproject.launcher.impl.TypeManagerImpl;

/**
 * {@link ExtensionContext} used by the worker.
 */
final class WorkerExtensionContext implements ExtensionContext {
  private static final String CLOUD_SETTINGS_PATH = "config/settings.yaml";
  private static final String CLOUD_ENV_SETTINGS_PATH = "config/env/settings.yaml";

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
      String tempSettings = "environment: LOCAL\ncloud: GOOGLE";
      InputStream in = new ByteArrayInputStream(tempSettings.getBytes(StandardCharsets.UTF_8));
      // InputStream cloudSettingsIn =
      //     WorkerModule.class.getClassLoader().getResourceAsStream(CLOUD_SETTINGS_PATH);
      // InputStream envCloudSettingsIn =
      //     WorkerModule.class.getClassLoader().getResourceAsStream(CLOUD_ENV_SETTINGS_PATH);
      // TODO: consider allowing env settings to override base settings. For now, concatenate them.
      // InputStream in = new SequenceInputStream(cloudSettingsIn, envCloudSettingsIn);
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
