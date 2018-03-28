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

import java.util.HashMap;
import java.util.Map;
import org.dataportabilityproject.api.launcher.Constants.Environment;
import org.dataportabilityproject.api.launcher.ExtensionContext;
import org.dataportabilityproject.api.launcher.Logger;
import org.dataportabilityproject.api.launcher.TypeManager;
import org.dataportabilityproject.config.extension.SettingsExtension;
import org.dataportabilityproject.config.extension.WorkerSettingsExtension;
import org.dataportabilityproject.launcher.impl.TypeManagerImpl;
import org.dataportabilityproject.types.transfer.auth.TokenAuthData;
import org.dataportabilityproject.types.transfer.auth.TokenSecretAuthData;
import org.dataportabilityproject.types.transfer.auth.TokensAndUrlAuthData;

/** {@link ExtensionContext} used by the worker. */
final class WorkerExtensionContext implements ExtensionContext {
  private final TypeManager typeManager;
  private final Map<Class<?>, Object> registered = new HashMap<>();
  private final SettingsExtension settingsExtension;

  WorkerExtensionContext(WorkerSettingsExtension settingsExtension) {
    this.typeManager = new TypeManagerImpl();
    typeManager.registerTypes(
        TokenAuthData.class, TokensAndUrlAuthData.class, TokenSecretAuthData.class);

    registered.put(TypeManager.class, typeManager);
    this.settingsExtension = settingsExtension;
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

  @Override
  public <T> T getSetting(String setting, T defaultValue) {
    return settingsExtension.getSetting(setting, defaultValue);
  }

  @Override
  public String cloud() {
    return settingsExtension.cloud();
  }

  @Override
  public Environment environment() {
    return settingsExtension.environment();
  }
}
