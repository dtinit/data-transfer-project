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

package org.datatransferproject.transfer;

import com.google.common.base.Preconditions;
import java.util.HashMap;
import java.util.Map;
import org.datatransferproject.api.launcher.Constants.Environment;
import org.datatransferproject.api.launcher.ExtensionContext;
import org.datatransferproject.api.launcher.Flag;
import org.datatransferproject.api.launcher.TypeManager;
import org.datatransferproject.config.extension.SettingsExtension;
import org.datatransferproject.launcher.impl.TypeManagerImpl;
import org.datatransferproject.types.transfer.auth.TokenAuthData;
import org.datatransferproject.types.transfer.auth.TokenSecretAuthData;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;

/**
 * {@link ExtensionContext} used by the transfer worker.
 */
public class WorkerExtensionContext implements ExtensionContext {

  private final TypeManager typeManager;
  private final Map<Class<?>, Object> registered = new HashMap<>();
  private final SettingsExtension settingsExtension;

  // Required settings
  private final String cloud;
  private final Environment environment;

  WorkerExtensionContext(SettingsExtension settingsExtension) {
    this.typeManager = new TypeManagerImpl();
    typeManager.registerTypes(
        TokenAuthData.class, TokensAndUrlAuthData.class, TokenSecretAuthData.class);

    registered.put(TypeManager.class, typeManager);
    this.settingsExtension = settingsExtension;

    cloud = settingsExtension.getSetting("cloud", null);
    Preconditions.checkNotNull(cloud, "Required setting 'cloud' is missing");
    environment = Environment.valueOf(settingsExtension.getSetting("environment", null));
    Preconditions.checkNotNull(environment, "Required setting 'environment' is missing");
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
  @Flag
  public String cloud() {
    return cloud;
  }

  @Override
  @Flag
  public Environment environment() {
    return environment;
  }
}
