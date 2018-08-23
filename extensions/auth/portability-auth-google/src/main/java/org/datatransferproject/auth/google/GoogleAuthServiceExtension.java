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
package org.datatransferproject.auth.google;

import com.google.api.client.http.HttpTransport;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.datatransferproject.api.launcher.ExtensionContext;
import org.datatransferproject.api.launcher.TypeManager;
import org.datatransferproject.spi.cloud.storage.AppCredentialStore;
import org.datatransferproject.spi.api.auth.AuthDataGenerator;
import org.datatransferproject.spi.api.auth.AuthServiceProviderRegistry.AuthMode;
import org.datatransferproject.spi.api.auth.extension.AuthServiceExtension;
import org.datatransferproject.types.transfer.auth.AppCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** An {@link AuthServiceExtension} providing authentication mechanism for Google services. */
public class GoogleAuthServiceExtension implements AuthServiceExtension {
  private static final Logger logger = LoggerFactory.getLogger(GoogleAuthServiceExtension.class);
  private static final String REDIRECT_PATH = "/callback/google";

  // TODO: share this between AuthServiceExtension and TransferExtension
  private static final ImmutableList<String> SUPPORTED_DATA_TYPES =
      ImmutableList.of("CONTACTS", "CALENDAR", "MAIL", "PHOTOS", "TASKS");

  // Map of AuthDataGenerators needed to import each data type
  private volatile Map<String, AuthDataGenerator> importAuthDataGenerators;
  // Map of AuthDataGenerators needed to export each data type
  private volatile Map<String, AuthDataGenerator> exportAuthDataGenerators;

  private AppCredentials appCredentials;
  private HttpTransport httpTransport;
  private TypeManager typeManager;
  private boolean initialized = false;

  public GoogleAuthServiceExtension() {}

  public String getServiceId() {
    return "GOOGLE";
  }

  public AuthDataGenerator getAuthDataGenerator(String transferDataType, AuthMode mode) {
    return getOrCreateAuthDataGenerator(transferDataType, mode);
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
    try {
      appCredentials =
          context
              .getService(AppCredentialStore.class)
              .getAppCredentials("GOOGLE_KEY", "GOOGLE_SECRET");
    } catch (IOException e) {
      logger.warn(
          "Problem getting AppCredentials: {}. Did you set GOOGLE_KEY and GOOGLE_SECRET?", e);
      return;
    }

    importAuthDataGenerators = new HashMap<>();
    exportAuthDataGenerators = new HashMap<>();
    httpTransport = context.getService(HttpTransport.class);
    typeManager = context.getService(TypeManager.class);
    initialized = true;
  }

  private synchronized AuthDataGenerator getOrCreateAuthDataGenerator(
      String transferDataType, AuthMode mode) {
    Preconditions.checkState(initialized, "Trying to getAuthDataGenerator before initialization");
    Preconditions.checkArgument(SUPPORTED_DATA_TYPES.contains(transferDataType));

    Map<String, AuthDataGenerator> generators =
        mode == AuthMode.EXPORT ? exportAuthDataGenerators : importAuthDataGenerators;

    if (!generators.containsKey(transferDataType)) {
      generators.put(
          transferDataType,
          new GoogleAuthDataGenerator(
                  appCredentials,
              httpTransport,
              typeManager.getMapper(),
              transferDataType,
              mode));
    }

    return generators.get(transferDataType);
  }
}
