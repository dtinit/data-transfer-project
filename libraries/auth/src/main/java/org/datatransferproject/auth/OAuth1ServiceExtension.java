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
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.datatransferproject.api.launcher.ExtensionContext;
import org.datatransferproject.spi.api.auth.AuthDataGenerator;
import org.datatransferproject.spi.api.auth.AuthServiceProviderRegistry.AuthMode;
import org.datatransferproject.spi.api.auth.extension.AuthServiceExtension;
import org.datatransferproject.spi.cloud.storage.AppCredentialStore;
import org.datatransferproject.types.transfer.auth.AppCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * General implementation of an {@link AuthServiceExtension} for OAuth2.  Largely exists to provide
 * the appropriate {@link OAuth1DataGenerator} upon request.
 */
public class OAuth1ServiceExtension implements AuthServiceExtension {

  private static final Logger logger = LoggerFactory.getLogger(OAuth1ServiceExtension.class);

  private final OAuth1Config oAuth1Config;

  private volatile Map<String, OAuth1DataGenerator> exportAuthDataGenerators;
  private volatile Map<String, OAuth1DataGenerator> importAuthDataGenerators;

  private AppCredentials appCredentials;
  private HttpTransport httpTransport;

  private boolean initialized = false;

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

    String serviceName = oAuth1Config.getServiceName().toUpperCase();

    try {
      appCredentials = context.getService(AppCredentialStore.class)
          .getAppCredentials(serviceName + "_KEY", serviceName + "_SECRET");
    } catch (IOException e) {
      logger.warn("Problem getting app credentials: {}.  Did you set {}_KEY and {}_SECRET?", e,
          serviceName, serviceName);
      return;
    }

    exportAuthDataGenerators = new HashMap<>();
    importAuthDataGenerators = new HashMap<>();
    httpTransport = context.getService(HttpTransport.class);
    initialized = true;
  }

  private synchronized OAuth1DataGenerator getOrCreateAuthDataGenerator(String transferType,
      AuthMode mode) {
    Preconditions.checkState(initialized, "Cannot get OAuth1DataGenerator before initialization");
    Preconditions.checkArgument(mode == AuthMode.EXPORT
        ? getExportTypes().contains(transferType)
        : getImportTypes().contains(transferType));

    Map<String, OAuth1DataGenerator> generators =
        mode == AuthMode.EXPORT ? exportAuthDataGenerators : importAuthDataGenerators;

    if (!generators.containsKey(transferType)) {
      generators.put(
          transferType,
          new OAuth1DataGenerator(oAuth1Config, appCredentials, httpTransport, transferType, mode)
      );
    }

    return generators.get(transferType);
  }
}
