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

package org.dataportabilityproject.transfer.twitter;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.List;
import org.dataportabilityproject.api.launcher.ExtensionContext;
import org.dataportabilityproject.spi.cloud.storage.AppCredentialStore;
import org.dataportabilityproject.spi.transfer.extension.TransferExtension;
import org.dataportabilityproject.spi.transfer.provider.Exporter;
import org.dataportabilityproject.spi.transfer.provider.Importer;
import org.dataportabilityproject.types.transfer.auth.AppCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TwitterTransferExtension implements TransferExtension {
  private static final List<String> SUPPORTED_TYPES = ImmutableList.of("photos");
  private static final Logger LOGGER = LoggerFactory.getLogger(TwitterTransferExtension.class);
  private static final String TWITTER_KEY = "TWITTER_KEY";
  private static final String TWITTER_SECRET = "TWITTER_SECRET";

  private TwitterPhotosImporter importer;
  private TwitterPhotosExporter exporter;
  private boolean initialized = false;

  @Override
  public String getServiceId() {
    return "Twitter";
  }

  @Override
  public Exporter<?, ?> getExporter(String transferDataType) {
    Preconditions.checkArgument(
        initialized, "Trying to call getExporter before initalizing TwitterTransferExtension");
    Preconditions.checkArgument(
        SUPPORTED_TYPES.contains(transferDataType),
        "Export of " + transferDataType + " not supported by Twitter");
    return exporter;
  }

  @Override
  public Importer<?, ?> getImporter(String transferDataType) {
    Preconditions.checkArgument(
        initialized, "Trying to call getExporter before initalizing TwitterTransferExtension");
    Preconditions.checkArgument(
        SUPPORTED_TYPES.contains(transferDataType),
        "Export of " + transferDataType + " not supported by Twitter");
    return importer;
  }

  @Override
  public void initialize(ExtensionContext context) {
    LOGGER.debug("starting twitter initialization");
    if (initialized) {
      LOGGER.warn("TwitterTransferExtension already initialized.");
      return;
    }

    AppCredentials appCredentials;
    try {
      appCredentials =
          context
              .getService(AppCredentialStore.class)
              .getAppCredentials(TWITTER_KEY, TWITTER_SECRET);
    } catch (IOException e) {
      LOGGER.warn(
          "Unable to retrieve client secrets. Did you set {} and {}?", TWITTER_KEY, TWITTER_SECRET);
      return;
    }

    exporter = new TwitterPhotosExporter(appCredentials);
    importer = new TwitterPhotosImporter(appCredentials);
    initialized = true;
  }
}
