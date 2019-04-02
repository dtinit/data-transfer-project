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

package org.datatransferproject.transfer;

import org.datatransferproject.api.launcher.DtpInternalMetricRecorder;
import org.datatransferproject.api.launcher.ExtensionContext;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.api.launcher.TypeManager;
import org.datatransferproject.config.extension.SettingsExtension;
import org.datatransferproject.launcher.metrics.ServiceAwareMetricRecorder;
import org.datatransferproject.launcher.types.TypeManagerImpl;
import org.datatransferproject.types.transfer.auth.TokenAuthData;
import org.datatransferproject.types.transfer.auth.TokenSecretAuthData;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;

import java.util.concurrent.ConcurrentHashMap;

/** A builder used to generate {@link ExtensionContext} used by the transfer worker. */
class WorkerExtensionContextBuilder {

  private final TypeManager typeManager;
  private final ConcurrentHashMap<Class<?>, Object> registered = new ConcurrentHashMap<>();
  private final SettingsExtension settingsExtension;
  private final Monitor monitor;
  private final DtpInternalMetricRecorder metricRecorder;

  WorkerExtensionContextBuilder(
      SettingsExtension settingsExtension,
      Monitor monitor,
      DtpInternalMetricRecorder metricRecorder) {
    this.monitor = monitor;
    this.metricRecorder = metricRecorder;
    this.typeManager = new TypeManagerImpl();
    typeManager.registerTypes(
        TokenAuthData.class, TokensAndUrlAuthData.class, TokenSecretAuthData.class);

    registered.put(TypeManager.class, typeManager);
    this.settingsExtension = settingsExtension;
  }

  TypeManager getTypeManager() {
    return typeManager;
  }

  SettingsExtension getSettingsExtension() {
    return settingsExtension;
  }

  Monitor getMonitor() {
    return monitor;
  }

  public <T> void registerService(Class<T> type, T service) {
    registered.put(type, service);
  }

  /**
   * Creates a {@link WorkerExtensionContext} not associated with a service. All instances of
   * {@link WorkerExtensionContext} generated from this builder will share the same registered
   * classes.
   */
  WorkerExtensionContext getWorkerExtensionContext() {
    return new WorkerExtensionContext(
        typeManager,
        registered,
        settingsExtension,
        monitor,
        new ServiceAwareMetricRecorder("Unknown", metricRecorder));
  }

  /**
   * Creates a {@link WorkerExtensionContext} associated with a service. All instances of
   * {@link WorkerExtensionContext} generated from this builder will share the same registered
   * classes.
   */
  WorkerExtensionContext getWorkerExtensionContextBuilderForService(String service) {
    return new WorkerExtensionContext(
        typeManager,
        registered,
        settingsExtension,
        monitor,
        new ServiceAwareMetricRecorder(service, metricRecorder));
  }
}
