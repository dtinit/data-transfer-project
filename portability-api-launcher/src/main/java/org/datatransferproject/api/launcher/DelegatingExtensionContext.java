/*
 * Copyright 2019 The Data Transfer Project Authors.
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
package org.datatransferproject.api.launcher;

import static java.lang.String.format;

import java.util.HashMap;
import java.util.Map;

/**
 * An {@link ExtensionContext} that is able to override registered services
 * to provide job/service specific values.
 */
public class DelegatingExtensionContext implements ExtensionContext {
  private final Map<Class<?>, Object> overriddenRegisteredClasses = new HashMap<>();
  private final ExtensionContext baseExtensionContext;

  public DelegatingExtensionContext(ExtensionContext baseExtensionContext) {
    this.baseExtensionContext = baseExtensionContext;
  }
  @Override
  public TypeManager getTypeManager() {
    return baseExtensionContext.getTypeManager();
  }

  @Override
  public Monitor getMonitor() {
    return baseExtensionContext.getMonitor();
  }

  @Override
  public <T> void registerService(Class<T> type, T service) {
    baseExtensionContext.registerService(type, service);
  }

  @Override
  public <T> T getService(Class<T> type) {
    if (overriddenRegisteredClasses.containsKey(type)) {
      return type.cast(overriddenRegisteredClasses.get(type));
    }
    return baseExtensionContext.getService(type);
  }

  @Override
  public <T> T getSetting(String setting, T defaultValue) {
    return baseExtensionContext.getSetting(setting, defaultValue);
  }

  @Override
  public String cloud() {
    return baseExtensionContext.cloud();
  }

  @Override
  public Constants.Environment environment() {
    return baseExtensionContext.environment();
  }

  public <T> void registerOverrideService(Class<T> type, T service) {
    if (overriddenRegisteredClasses.containsKey(type)) {
      getMonitor()
          .info(
              () ->
                  format(
                      "Re-overriding type %s from %s to %s",
                      type, overriddenRegisteredClasses.get(type), service));
    }
    overriddenRegisteredClasses.put(type, service);
  }
}
