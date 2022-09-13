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

package org.datatransferproject.transfer.twitter;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import org.datatransferproject.api.launcher.ExtensionContext;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.spi.cloud.storage.AppCredentialStore;
import org.datatransferproject.types.common.models.DataVertical;
import org.datatransferproject.spi.transfer.extension.TransferExtension;
import org.datatransferproject.spi.transfer.provider.Exporter;
import org.datatransferproject.spi.transfer.provider.Importer;
import org.datatransferproject.types.transfer.auth.AppCredentials;

import java.io.IOException;
import java.util.List;

import static java.lang.String.format;
import static org.datatransferproject.types.common.models.DataVertical.PHOTOS;

/**
 * Extension to allow Twitter content to be transferred.
 *
 * <p>Currently only photos content to be exported/imported.
 *
 * <p>In the future if a social vertical gets defined support should be added for that.
 */
public class TwitterTransferExtension implements TransferExtension {
  private static final List<DataVertical> SUPPORTED_TYPES = ImmutableList.of(PHOTOS);
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
  public Exporter<?, ?> getExporter(DataVertical transferDataType) {
    Preconditions.checkArgument(
        initialized, "Trying to call getExporter before initalizing TwitterTransferExtension");
    Preconditions.checkArgument(
        SUPPORTED_TYPES.contains(transferDataType),
        "Export of " + transferDataType + " not supported by Twitter");
    return exporter;
  }

  @Override
  public Importer<?, ?> getImporter(DataVertical transferDataType) {
    Preconditions.checkArgument(
        initialized, "Trying to call getImporter before initalizing TwitterTransferExtension");
    Preconditions.checkArgument(
        SUPPORTED_TYPES.contains(transferDataType),
        "Import of " + transferDataType + " not supported by Twitter");
    return importer;
  }

  @Override
  public void initialize(ExtensionContext context) {
    Monitor monitor = context.getMonitor();
    monitor.debug(() -> "Starting Twitter initialization");
    if (initialized) {
      monitor.severe(() -> "TwitterTransferExtension already initialized.");
      return;
    }

    AppCredentials appCredentials;
    try {
      appCredentials =
          context
              .getService(AppCredentialStore.class)
              .getAppCredentials(TWITTER_KEY, TWITTER_SECRET);
    } catch (IOException e) {
      monitor.info(
          () ->
              format(
                  "Unable to retrieve Twitter AppCredentials. Did you set %s and %s?",
                  TWITTER_KEY, TWITTER_SECRET),
          e);
      return;
    }

    exporter = new TwitterPhotosExporter(appCredentials, monitor);
    importer = new TwitterPhotosImporter(appCredentials, monitor);
    initialized = true;
  }
}
