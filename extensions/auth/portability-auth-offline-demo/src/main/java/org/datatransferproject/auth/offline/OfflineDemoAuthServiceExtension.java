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
package org.datatransferproject.auth.offline;

import org.datatransferproject.api.launcher.ExtensionContext;
import org.datatransferproject.spi.api.auth.AuthDataGenerator;
import org.datatransferproject.spi.api.auth.AuthServiceProviderRegistry;
import org.datatransferproject.spi.api.auth.extension.AuthServiceExtension;

import java.util.Collections;
import java.util.List;

/**
 * Provides an extension that demonstrates how import of offline data can be implemented.
 *
 * <p>The extension receives demo offline data and simulates importing it by printing the data to
 * the console.
 */
public class OfflineDemoAuthServiceExtension implements AuthServiceExtension {
  private static final String SERVICE_ID = "OFFLINE-DEMO";

  private static final List<String> SUPPORTED_SERVICES = Collections.singletonList("OFFLINE-DATA");

  @Override
  public String getServiceId() {
    return SERVICE_ID;
  }

  @Override
  public AuthDataGenerator getAuthDataGenerator(
      String type, AuthServiceProviderRegistry.AuthMode mode) {
    return new OfflineDemoAuthDataGenerator();
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
  public void initialize(ExtensionContext context) {}
}
