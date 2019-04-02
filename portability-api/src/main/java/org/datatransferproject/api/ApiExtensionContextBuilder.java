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

import org.datatransferproject.api.launcher.DtpInternalMetricRecorder;
import org.datatransferproject.api.launcher.ExtensionContext;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.api.launcher.TypeManager;
import org.datatransferproject.config.extension.SettingsExtension;
import org.datatransferproject.launcher.metrics.ServiceAwareMetricRecorder;

import java.util.concurrent.ConcurrentHashMap;

/** A builder used to generate {@link ExtensionContext} used by the API. */
public class ApiExtensionContextBuilder  {

  private final ConcurrentHashMap<Class<?>, Object> registered = new ConcurrentHashMap<>();
  private final TypeManager typeManager;
  private final SettingsExtension settingsExtension;
  private final Monitor monitor;
  private final DtpInternalMetricRecorder internalMetricRecorder;

  public ApiExtensionContextBuilder(
      TypeManager typeManager,
      SettingsExtension settingsExtension,
      Monitor monitor,
      DtpInternalMetricRecorder internalMetricRecorder) {
    this.typeManager = typeManager;
    this.settingsExtension = settingsExtension;
    this.monitor = monitor;
    this.internalMetricRecorder = internalMetricRecorder;
    registered.put(TypeManager.class, typeManager);
  }

  public <T> void registerService(Class<T> type, T service) {
    registered.put(type, service);
  }

  /**
   * Creates a {@link ApiExtensionContext} associated with a service. All instances of
   * {@link ApiExtensionContext} generated from this builder will share the same registered
   * classes.
   */
  public ApiExtensionContext getSharedApiExtensionContextForService(String service) {
    return new ApiExtensionContext(
        typeManager,
        registered,
        settingsExtension,
        monitor,
        new ServiceAwareMetricRecorder(service, internalMetricRecorder));
  }

  /**
   * Creates a {@link ApiExtensionContext} not associated with a service. All instances of
   * {@link ApiExtensionContext} generated from this builder will share the same registered
   * classes.
   */
  public ApiExtensionContext getSharedApiExtensionContext() {
    return new ApiExtensionContext(
        typeManager,
        registered,
        settingsExtension,
        monitor,
        new ServiceAwareMetricRecorder("Unknown", internalMetricRecorder));
  }
}
