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

package org.datatransferproject.auth.facebook;

import com.google.api.client.http.HttpTransport;
import com.google.common.collect.ImmutableList;
import org.datatransferproject.api.launcher.ExtensionContext;
import org.datatransferproject.spi.api.auth.AuthDataGenerator;
import org.datatransferproject.spi.api.auth.AuthServiceProviderRegistry;
import org.datatransferproject.spi.api.auth.extension.AuthServiceExtension;
import org.datatransferproject.spi.cloud.storage.AppCredentialStore;
import org.datatransferproject.types.transfer.auth.AppCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

public class FacebookAuthServiceExtension implements AuthServiceExtension {
  private static final Logger logger = LoggerFactory.getLogger(FacebookAuthServiceExtension.class);

  private static final ImmutableList<String> SUPPORTED_DATA_TYPES = ImmutableList.of("PHOTOS");

  private boolean initialized = false;

  private FacebookAuthDataGenerator authDataGenerator;

  public FacebookAuthServiceExtension() {}

  @Override
  public String getServiceId() {
    return "Facebook";
  }

  @Override
  public AuthDataGenerator getAuthDataGenerator(
      String transferDataType, AuthServiceProviderRegistry.AuthMode mode) {
    return authDataGenerator;
  }

  @Override
  public List<String> getImportTypes() {
    return SUPPORTED_DATA_TYPES;
  }

  @Override
  public List<String> getExportTypes() {
    return SUPPORTED_DATA_TYPES;
  }

  @Override
  public void initialize(ExtensionContext context) {
    if (initialized) return;

    AppCredentials appCredentials;
    try {
      appCredentials =
          context
              .getService(AppCredentialStore.class)
              .getAppCredentials("FACEBOOK_KEY", "FACEBOOK_SECRET");
    } catch (IOException e) {
      logger.warn(
          "Problem getting AppCredentials: {}. Did you set FACEBOOK_KEY and FACEBOOK_SECRET?", e);
      return;
    }

    authDataGenerator =
        new FacebookAuthDataGenerator(appCredentials, context.getService(HttpTransport.class));

    initialized = true;
  }
}
