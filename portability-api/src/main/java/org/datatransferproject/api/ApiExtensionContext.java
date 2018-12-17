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
package org.datatransferproject.api;

import com.google.common.base.Preconditions;
import org.datatransferproject.api.launcher.Constants.Environment;
import org.datatransferproject.api.launcher.ExtensionContext;
import org.datatransferproject.api.launcher.Flag;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.api.launcher.TypeManager;
import org.datatransferproject.config.extension.SettingsExtension;

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
  private final SettingsExtension settingsExtension;
  private final Monitor monitor;

  // Required settings
  private final String cloud;
  private final Environment environment;
  private final String baseUrl;
  private final String baseApiUrl;

  public ApiExtensionContext(TypeManager typeManager, SettingsExtension settingsExtension, Monitor monitor) {
    this.typeManager = typeManager;
    this.settingsExtension = settingsExtension;
    this.monitor = monitor;
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
  public TypeManager getTypeManager() {
    return typeManager;
  }

  @Override
  public Monitor getMonitor() {
    return monitor;
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
