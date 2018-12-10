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

package org.datatransferproject.auth;

import com.google.api.client.http.HttpTransport;
import com.google.common.base.Preconditions;
import org.datatransferproject.api.launcher.ExtensionContext;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.spi.api.auth.AuthDataGenerator;
import org.datatransferproject.spi.api.auth.AuthServiceProviderRegistry.AuthMode;
import org.datatransferproject.spi.api.auth.extension.AuthServiceExtension;
import org.datatransferproject.spi.cloud.storage.AppCredentialStore;
import org.datatransferproject.types.transfer.auth.AppCredentials;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;

/**
 * General implementation of an {@link AuthServiceExtension} for OAuth1. Largely exists to provide
 * the appropriate {@link OAuth1DataGenerator} upon request.
 */
public class OAuth1ServiceExtension implements AuthServiceExtension {

  private final OAuth1Config oAuth1Config;

  private volatile Map<String, OAuth1DataGenerator> exportAuthDataGenerators;
  private volatile Map<String, OAuth1DataGenerator> importAuthDataGenerators;

  private AppCredentials appCredentials;
  private HttpTransport httpTransport;

  private boolean initialized = false;
  private Monitor monitor;

  public OAuth1ServiceExtension(OAuth1Config oAuth1Config) {
    this.oAuth1Config = oAuth1Config;
  }

  @Override
  public String getServiceId() {
    return oAuth1Config.getServiceName();
  }

  @Override
  public AuthDataGenerator getAuthDataGenerator(String transferDataType, AuthMode mode) {
    return getOrCreateAuthDataGenerator(transferDataType, mode);
  }

  @Override
  public List<String> getImportTypes() {
    return new ArrayList<>(oAuth1Config.getImportScopes().keySet());
  }

  @Override
  public List<String> getExportTypes() {
    return new ArrayList<>(oAuth1Config.getExportScopes().keySet());
  }

  @Override
  public void initialize(ExtensionContext context) {
    if (initialized) {
      return;
    }

    monitor = context.getMonitor();
    String serviceName = oAuth1Config.getServiceName().toUpperCase();

    String keyName = serviceName + "_KEY";
    String secretName = serviceName + "_SECRET";
    try {
      appCredentials =
          context.getService(AppCredentialStore.class).getAppCredentials(keyName, secretName);
    } catch (IOException e) {
      monitor.info(
          () ->
              format(
                  "Unable to retrieve OAuth1 AppCredentials. Did you set %s and %s?",
                  keyName, secretName),
          e);

      return;
    }

    exportAuthDataGenerators = new HashMap<>();
    importAuthDataGenerators = new HashMap<>();
    httpTransport = context.getService(HttpTransport.class);
    initialized = true;
  }

  private synchronized OAuth1DataGenerator getOrCreateAuthDataGenerator(
      String transferType, AuthMode mode) {
    Preconditions.checkState(initialized, "Cannot get OAuth1DataGenerator before initialization");
    Preconditions.checkArgument(
        mode == AuthMode.EXPORT
            ? getExportTypes().contains(transferType)
            : getImportTypes().contains(transferType));

    Map<String, OAuth1DataGenerator> generators =
        mode == AuthMode.EXPORT ? exportAuthDataGenerators : importAuthDataGenerators;

    if (!generators.containsKey(transferType)) {
      generators.put(
          transferType,
          new OAuth1DataGenerator(
              oAuth1Config, appCredentials, httpTransport, transferType, mode, monitor));
    }

    return generators.get(transferType);
  }
}
