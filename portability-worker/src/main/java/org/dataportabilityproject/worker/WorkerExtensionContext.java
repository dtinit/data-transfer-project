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

import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import org.dataportabilityproject.api.launcher.ExtensionContext;
import org.dataportabilityproject.api.launcher.Logger;
import org.dataportabilityproject.api.launcher.TypeManager;
import org.dataportabilityproject.config.ConfigUtils;
import org.dataportabilityproject.launcher.impl.TypeManagerImpl;
import org.dataportabilityproject.types.transfer.auth.TokenAuthData;
import org.dataportabilityproject.types.transfer.auth.TokensAndUrlAuthData;

/**
 * {@link ExtensionContext} used by the worker.
 */
final class WorkerExtensionContext implements ExtensionContext {
  /**
   * Custom settings for an extension. This is separate from the required {@code CommonSettings}.
   */
  private final Map<String, Object> config;

  private final TypeManager typeManager;
  private final Map<Class<?>, Object> registered = new HashMap<>();

  WorkerExtensionContext() {
    this.typeManager = new TypeManagerImpl();
    typeManager.registerType(TokenAuthData.class);
    typeManager.registerType(TokensAndUrlAuthData.class);

    registered.put(TypeManager.class, typeManager);
    config = parseExtensionSettings();
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
    return type.cast(registered.get(type));
  }

  @Override
  public <T> void registerService(Class<T> type, T service) {
    registered.put(type, service);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> T getConfiguration(String key, T defaultValue) {
    if (config.containsKey(key)) {
      return (T) config.get(key);
    }
    return defaultValue;
  }

  private Map<String, Object> parseExtensionSettings() {
    try {
      // TODO: this should add the contents of an extension's custom config directory (TBD), which
      // allows extensions to structure their custom settings however they want.
      ImmutableList<String> settingsFiles = ImmutableList.<String>builder().build();
      InputStream in = ConfigUtils.getSettingsCombinedInputStream(settingsFiles);
      return ConfigUtils.parse(in);
    } catch (IOException e) {
      throw new IllegalArgumentException("Problem parsing cloud extension settings", e);
    }
  }
}
