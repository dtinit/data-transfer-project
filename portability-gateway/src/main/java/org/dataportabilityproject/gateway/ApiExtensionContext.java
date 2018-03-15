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
package org.dataportabilityproject.gateway;

import org.dataportabilityproject.api.launcher.ExtensionContext;
import org.dataportabilityproject.api.launcher.Logger;
import org.dataportabilityproject.api.launcher.TypeManager;

import java.util.HashMap;
import java.util.Map;

/**
 * Provides a context for initializing extensions.
 *
 * <p>
 */
public class ApiExtensionContext implements ExtensionContext {
  private final Map<Class<?>, Object> registered = new HashMap<>();
  private final TypeManager typeManager;
  private final Map<String, Object> configuration;

  public ApiExtensionContext(TypeManager typeManager, Map<String, Object> configuration) {
    this.configuration = configuration;
    this.typeManager = typeManager;
    registered.put(TypeManager.class, typeManager);
  }

  @Override
  public Logger getLogger() {
    // TODO implement
    return null;
  }

  @Override
  public TypeManager getTypeManager() {
    return typeManager;
  }

  @Override
  public <T> T getService(Class<T> type) {
    return type.cast(registered.get(type));
  }

  @Override
  public <T> void registerService(Class<T> type, T service) {
    registered.put(type, service);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T getConfiguration(final String key, final T defaultValue) {
    return (T) configuration.getOrDefault(key, defaultValue);
  }
}
