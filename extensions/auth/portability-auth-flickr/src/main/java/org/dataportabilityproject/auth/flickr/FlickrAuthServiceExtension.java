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

package org.dataportabilityproject.auth.flickr;

import com.google.common.collect.ImmutableList;
import org.dataportabilityproject.api.launcher.ExtensionContext;
import org.dataportabilityproject.spi.gateway.auth.AuthDataGenerator;
import org.dataportabilityproject.spi.gateway.auth.AuthServiceProviderRegistry;
import org.dataportabilityproject.spi.gateway.auth.extension.AuthServiceExtension;

import java.util.List;

public class FlickrAuthServiceExtension implements AuthServiceExtension {
  private final static String SERVICE_ID = "flickr";
  private final List<String> supportedServices = ImmutableList.of("photos");
  private FlickrAuthDataGenerator flickrAuthDataGenerator;

  @Override
  public String getServiceId() {
    return SERVICE_ID;
  }

  @Override
  public AuthDataGenerator getAuthDataGenerator(String transferDataType, AuthServiceProviderRegistry.AuthMode mode) {
    return flickrAuthDataGenerator;
  }

  @Override
  public List<String> getImportTypes() {
    return supportedServices;
  }

  @Override
  public List<String> getExportTypes() {
    return supportedServices;
  }

  @Override
  public void initialize(ExtensionContext context) {


  }
}
