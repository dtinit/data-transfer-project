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
package org.dataportabilityproject.auth.microsoft;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import okhttp3.OkHttpClient;
import org.dataportabilityproject.api.launcher.ExtensionContext;
import org.dataportabilityproject.spi.cloud.storage.AppCredentialStore;
import org.dataportabilityproject.spi.gateway.auth.AuthDataGenerator;
import org.dataportabilityproject.spi.gateway.auth.AuthServiceProviderRegistry.AuthMode;
import org.dataportabilityproject.spi.gateway.auth.extension.AuthServiceExtension;
import org.dataportabilityproject.types.transfer.auth.AppCredentials;

import java.io.IOException;
import java.util.List;

/** */
public class MicrosoftAuthServiceExtension implements AuthServiceExtension {
  private static final String REDIRECT_PATH = "/callback/microsoft";
  private static final ImmutableList<String> SUPPORTED_SERVICES =
      ImmutableList.<String>builder().add("calendar", "contacts").build();

  private MicrosoftAuthDataGenerator authDataGenerator;

  public String getServiceId() {
    return "microsoft";
  }

  public AuthDataGenerator getAuthDataGenerator(String transferDataType, AuthMode mode) {
    Preconditions.checkNotNull(authDataGenerator);
    return authDataGenerator;
  }

  @Override
  public List<String> getImportTypes() {
    return SUPPORTED_SERVICES;
  }

  @Override
  public List<String> getExportTypes() {
    return SUPPORTED_SERVICES;
  }

  @Override
  public void initialize(ExtensionContext context) {
    ObjectMapper mapper = context.getTypeManager().getMapper();

    OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder();
    OkHttpClient okHttpClient = clientBuilder.build();

    AppCredentialStore appCredentialStore = context.getService(AppCredentialStore.class);
    try {
      AppCredentials credentials =
          appCredentialStore.getAppCredentials("MICROSOFT_KEY", "MICROSOFT_SECRET");
      if (credentials == null) {
        throw new IllegalStateException("Microsoft Graph API credentials not found");
      }
      authDataGenerator =
          new MicrosoftAuthDataGenerator(
              REDIRECT_PATH, credentials::getKey, credentials::getSecret, okHttpClient, mapper);
    } catch (IOException e) {
      throw new IllegalStateException(
          "Error retrieving Microsoft Graph API credentials - Were they set?", e);
    }
  }
}
