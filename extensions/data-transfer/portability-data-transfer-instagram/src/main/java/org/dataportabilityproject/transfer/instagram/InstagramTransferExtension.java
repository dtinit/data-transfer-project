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
package org.dataportabilityproject.transfer.instagram;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.http.HttpTransport;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import org.dataportabilityproject.api.launcher.ExtensionContext;
import org.dataportabilityproject.spi.cloud.storage.AppCredentialStore;
import org.dataportabilityproject.spi.cloud.storage.JobStore;
import org.dataportabilityproject.spi.transfer.extension.TransferExtension;
import org.dataportabilityproject.spi.transfer.provider.Exporter;
import org.dataportabilityproject.spi.transfer.provider.Importer;
import org.dataportabilityproject.transfer.instagram.photos.InstagramPhotoExporter;
import org.dataportabilityproject.transfer.instagram.photos.InstagramPhotoImporter;
import org.dataportabilityproject.types.transfer.auth.AppCredentials;
import org.dataportabilityproject.types.transfer.auth.TokensAndUrlAuthData;
import org.dataportabilityproject.types.transfer.models.photos.PhotosContainerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InstagramTransferExtension implements TransferExtension {
  private final Logger logger = LoggerFactory.getLogger(InstagramTransferExtension.class);
  private static final ImmutableList<String> SUPPORTED_DATA_TYPES = ImmutableList.of("photos");
  // The identifiers in the key management store for the api client key and secret
  private static final String INSTAGRAM_KEY = "INSTAGRAM_KEY";
  private static final String INSTAGRAM_SECRET = "INSTAGRAM_SECRET";
  private Exporter<TokensAndUrlAuthData, PhotosContainerResource> exporter;
  private Importer<TokensAndUrlAuthData, PhotosContainerResource> importer;

  private boolean initialized = false;

  @Override
  public String getServiceId() {
    return "instagram";
  }

  @Override
  public Exporter<?, ?> getExporter(String transferDataType) {
    Preconditions.checkArgument(
        initialized, "InstagramTransferExtension not initialized. Unable to get Exporter");
    Preconditions.checkArgument(SUPPORTED_DATA_TYPES.contains(transferDataType));
    return exporter;
  }

  @Override
  public Importer<?, ?> getImporter(String transferDataType) {
    Preconditions.checkArgument(
        initialized, "InstagramTransferExtension not initialized. Unable to get Importer");
    Preconditions.checkArgument(SUPPORTED_DATA_TYPES.contains(transferDataType));
    return importer;
  }

  @Override
  public void initialize(ExtensionContext context) {
    if (initialized) {
      logger.warn("InstagramTransferExtension already initalized");
      return;
    }

    ObjectMapper mapper =
        new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    JobStore jobStore = context.getService(JobStore.class);
    HttpTransport httpTransport = context.getService(HttpTransport.class);
    AppCredentials credentials;
    try {
      credentials =
          context
              .getService(AppCredentialStore.class)
              .getAppCredentials(INSTAGRAM_KEY, INSTAGRAM_SECRET);
    } catch (IOException e) {
      logger.warn(
          "Error retrieving Instagram Credentials. Did you set {} and {}?",
          INSTAGRAM_KEY,
          INSTAGRAM_SECRET);
      return;
    }

    exporter = new InstagramPhotoExporter(mapper, httpTransport);
    importer = new InstagramPhotoImporter();

    initialized = true;
  }
}
