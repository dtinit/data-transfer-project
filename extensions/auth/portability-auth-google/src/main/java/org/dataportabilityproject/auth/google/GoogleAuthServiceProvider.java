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

import com.google.common.collect.ImmutableList;
import java.util.List;
import org.dataportabilityproject.spi.gateway.auth.AuthDataGenerator;
import org.dataportabilityproject.spi.gateway.auth.AuthServiceProvider;
import org.dataportabilityproject.spi.gateway.auth.AuthServiceProviderRegistry.AuthMode;

/** */
public class GoogleAuthServiceProvider implements AuthServiceProvider {

  public GoogleAuthServiceProvider() {
  }

  public String getServiceId() {
    return "google";
  }
  

  public AuthDataGenerator getAuthDataGenerator(String transferDataType, AuthMode mode) {
    // TODO
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
}
