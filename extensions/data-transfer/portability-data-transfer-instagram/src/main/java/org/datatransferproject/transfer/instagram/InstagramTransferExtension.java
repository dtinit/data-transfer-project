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
package org.datatransferproject.transfer.instagram;

import com.google.api.client.http.HttpTransport;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import org.datatransferproject.api.launcher.ExtensionContext;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.spi.cloud.storage.AppCredentialStore;
import org.datatransferproject.spi.transfer.extension.TransferExtension;
import org.datatransferproject.spi.transfer.provider.Exporter;
import org.datatransferproject.spi.transfer.provider.Importer;
import org.datatransferproject.transfer.instagram.photos.InstagramPhotoExporter;
import org.datatransferproject.transfer.instagram.videos.InstagramVideoExporter;
import org.datatransferproject.types.transfer.auth.AppCredentials;

public class InstagramTransferExtension implements TransferExtension {
  private static final ImmutableList<String> SUPPORTED_DATA_TYPES =
      ImmutableList.of("PHOTOS", "VIDEOS");

  private ImmutableMap<String, Exporter> exporterMap;

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
    return exporterMap.get(transferDataType);
  }

  @Override
  public Importer<?, ?> getImporter(String transferDataType) {
    Preconditions.checkArgument(
        initialized, "InstagramTransferExtension not initialized. Unable to get Importer");
    Preconditions.checkArgument(false, "Instagram does not support import");
    return null;
  }

  @Override
  public void initialize(ExtensionContext context) {
    if (initialized) {
      Monitor monitor = context.getMonitor();
      monitor.severe(() -> "InstagramTransferExtension already initialized");
      return;
    }

    AppCredentials appCredentials;
    final Monitor monitor = context.getMonitor();
    try {
      appCredentials =
          context
              .getService(AppCredentialStore.class)
              .getAppCredentials("INSTAGRAM_KEY", "INSTAGRAM_SECRET");
    } catch (IOException e) {
      monitor.info(
          () ->
              "Unable to retrieve Instagram AppCredentials. Did you set INSTAGRAM_KEY, INSTAGRAM_SECRET?",
          e);
      return;
    }

    HttpTransport httpTransport = context.getService(HttpTransport.class);

    ImmutableMap.Builder<String, Exporter> exporterBuilder = ImmutableMap.builder();
    exporterBuilder.put(
        "PHOTOS", new InstagramPhotoExporter(httpTransport, monitor, appCredentials));
    exporterBuilder.put(
        "VIDEOS", new InstagramVideoExporter(httpTransport, monitor, appCredentials));
    exporterMap = exporterBuilder.build();
    initialized = true;
  }
}
