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
package org.dataportabilityproject.api;

import com.google.common.base.Preconditions;
import java.util.HashMap;
import java.util.Map;
import org.dataportabilityproject.api.launcher.Constants.Environment;
import org.dataportabilityproject.api.launcher.ExtensionContext;
import org.dataportabilityproject.api.launcher.Logger;
import org.dataportabilityproject.api.launcher.TypeManager;
import org.dataportabilityproject.api.launcher.Flag;
import org.dataportabilityproject.config.extension.SettingsExtension;

/**
 * Provides a context for initializing extensions.
 *
 * <p>
 */
public class ApiExtensionContext implements ExtensionContext {
  private final Map<Class<?>, Object> registered = new HashMap<>();
  private final TypeManager typeManager;
  private final SettingsExtension settingsExtension;

  // Required settings
  private final String cloud;
  private final Environment environment;
  private final String baseUrl;
  private final String baseApiUrl;

  public ApiExtensionContext(TypeManager typeManager, SettingsExtension settingsExtension) {
    this.typeManager = typeManager;
    this.settingsExtension = settingsExtension;
    registered.put(TypeManager.class, typeManager);

    cloud = settingsExtension.getSetting("cloud", null);
    Preconditions.checkNotNull(cloud, "Required setting 'cloud' is missing");
    environment = Environment.valueOf(settingsExtension.getSetting("environment", null));
    Preconditions.checkNotNull(environment, "Required setting 'environment' is missing");
    baseUrl = settingsExtension.getSetting("baseUrl", null);
    Preconditions.checkNotNull(baseUrl, "Required setting 'baseUrl' is missing");
    baseApiUrl = settingsExtension.getSetting("baseApiUrl", null);
    Preconditions.checkNotNull(baseApiUrl, "Required setting 'baseApiUrl' is missing");
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
  public <T> T getSetting(String setting, T defaultValue) {
    return settingsExtension.getSetting(setting, defaultValue);
  }

  @Override
  @Flag
  public String cloud() {
    return cloud;
  }

  @Override
  @Flag
  public Environment environment() {
    return environment;
  }

  @Flag
  public String baseUrl() {
    return baseUrl;
  }

  @Flag
  public String baseApiUrl() {
    return baseApiUrl;
  }
}
