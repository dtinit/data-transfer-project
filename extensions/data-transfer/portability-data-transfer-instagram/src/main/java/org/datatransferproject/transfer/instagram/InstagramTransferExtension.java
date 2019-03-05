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

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.http.HttpTransport;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import org.datatransferproject.api.launcher.ExtensionContext;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.spi.transfer.extension.TransferExtension;
import org.datatransferproject.spi.transfer.provider.Exporter;
import org.datatransferproject.spi.transfer.provider.Importer;
import org.datatransferproject.transfer.instagram.photos.InstagramPhotoExporter;
import org.datatransferproject.types.common.models.photos.PhotosContainerResource;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;


public class InstagramTransferExtension implements TransferExtension {
  private static final ImmutableList<String> SUPPORTED_DATA_TYPES = ImmutableList.of("PHOTOS");

  private Exporter<TokensAndUrlAuthData, PhotosContainerResource> exporter;

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

    ObjectMapper mapper =
        new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    HttpTransport httpTransport = context.getService(HttpTransport.class);
    exporter = new InstagramPhotoExporter(mapper, httpTransport);
    initialized = true;
  }
}
