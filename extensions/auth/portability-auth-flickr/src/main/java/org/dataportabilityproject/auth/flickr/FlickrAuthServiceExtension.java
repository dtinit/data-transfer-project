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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.List;
import org.dataportabilityproject.api.launcher.ExtensionContext;
import org.dataportabilityproject.spi.cloud.storage.AppCredentialStore;
import org.dataportabilityproject.spi.api.auth.AuthDataGenerator;
import org.dataportabilityproject.spi.api.auth.AuthServiceProviderRegistry;
import org.dataportabilityproject.spi.api.auth.extension.AuthServiceExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlickrAuthServiceExtension implements AuthServiceExtension {
  private static final String FLICKR_KEY = "FLICKR_KEY";
  private static final String FLICKR_SECRET = "FLICKR_SECRET";
  private static final String SERVICE_ID = "flickr";

  private final Logger logger = LoggerFactory.getLogger(FlickrAuthServiceExtension.class);
  private final List<String> supportedServices = ImmutableList.of("photos");
  private FlickrAuthDataGenerator flickrAuthDataGenerator;
  private boolean initialized = false;

  @Override
  public String getServiceId() {
    return SERVICE_ID;
  }

  @Override
  public AuthDataGenerator getAuthDataGenerator(
      String transferDataType, AuthServiceProviderRegistry.AuthMode mode) {
    Preconditions.checkArgument(
        initialized,
        "FlickrAuthServiceExtension is not initialized! Unable to retrieve AuthDataGenerator");
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
    AppCredentialStore appCredentialStore = context.getService(AppCredentialStore.class);

    try {
      flickrAuthDataGenerator =
          new FlickrAuthDataGenerator(
              appCredentialStore.getAppCredentials(FLICKR_KEY, FLICKR_SECRET));
      initialized = true;
    } catch (IOException e) {
      logger.debug(
          "Error retrieving Flickr Credentials. Did you set {} and {}?", FLICKR_KEY, FLICKR_SECRET);
    }
  }
}
