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
package org.dataportabilityproject.auth.derived;

import org.dataportabilityproject.api.launcher.ExtensionContext;
import org.dataportabilityproject.spi.api.auth.AuthDataGenerator;
import org.dataportabilityproject.spi.api.auth.AuthServiceProviderRegistry;
import org.dataportabilityproject.spi.api.auth.extension.AuthServiceExtension;

import java.util.Collections;
import java.util.List;

/**
 * Provides an extension that demonstrates how import of derived data can be implemented.
 *
 * <p>The extension receives demo derived data and simulates importing it by printing the data to
 * the console.
 */
public class DerivedDemoAuthServiceExtension implements AuthServiceExtension {
  private static final String SERVICE_ID = "derived-demo";

  private static final List<String> SUPPORTED_SERVICES = Collections.singletonList("derived-data");

  @Override
  public String getServiceId() {
    return SERVICE_ID;
  }

  @Override
  public AuthDataGenerator getAuthDataGenerator(
      String type, AuthServiceProviderRegistry.AuthMode mode) {
    return new DerivedDemoAuthDataGenerator();
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
