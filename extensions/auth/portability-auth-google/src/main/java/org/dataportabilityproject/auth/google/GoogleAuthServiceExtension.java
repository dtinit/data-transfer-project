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
package org.dataportabilityproject.auth.google;

import com.google.api.client.http.HttpTransport;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.List;
import org.dataportabilityproject.api.launcher.ExtensionContext;
import org.dataportabilityproject.spi.cloud.storage.AppCredentialStore;
import org.dataportabilityproject.spi.gateway.auth.AuthDataGenerator;
import org.dataportabilityproject.spi.gateway.auth.AuthServiceProviderRegistry.AuthMode;
import org.dataportabilityproject.spi.gateway.auth.extension.AuthServiceExtension;
import org.dataportabilityproject.types.transfer.auth.AppCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** */
public class GoogleAuthServiceExtension implements AuthServiceExtension {
  private static final Logger logger = LoggerFactory.getLogger(GoogleAuthServiceExtension.class);
  private static final String REDIRECT_PATH = "/callback/google";
  private static final ImmutableList<String> DEFAULT_AUTH_GENERATOR_SERVICES =
      ImmutableList.of("contacts", "calendar");
  private volatile GoogleAuthDataGenerator authDataGenerator;

  public GoogleAuthServiceExtension() {}

  public String getServiceId() {
    return "google";
  }

  public AuthDataGenerator getAuthDataGenerator(String transferDataType, AuthMode mode) {
    // TODO Create auth data generator for a given mode, usually means a different scope
    if (DEFAULT_AUTH_GENERATOR_SERVICES.contains(transferDataType)) {
      return authDataGenerator;
    }
    throw new UnsupportedOperationException();
  }

  @Override
  public List<String> getImportTypes() {
    return ImmutableList.of();
  }

  @Override
  public List<String> getExportTypes() {
    return ImmutableList.of();
  }

  @Override
  public void initialize(ExtensionContext context) {

    makeAuthDataGenerator(context);
  }

  private synchronized GoogleAuthDataGenerator makeAuthDataGenerator(ExtensionContext context) {
    if (authDataGenerator == null) {
      AppCredentials credentials;
      try {
        credentials =
            context
                .getService(AppCredentialStore.class)
                .getAppCredentials("GOOGLE_KEY", "GOOGLE_SECRET");
      } catch (IOException e) {
        logger.warn("Problem getting AppCredentials: {}", e);
        return null;
      }

      authDataGenerator =
          new GoogleAuthDataGenerator(
              REDIRECT_PATH,
              credentials.getKey(),
              credentials.getSecret(),
              context.getService(HttpTransport.class),
              context.getTypeManager().getMapper());
    }
    return authDataGenerator;
  }
}
