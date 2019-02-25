/*
 * Copyright 2019 The Data Transfer Project Authors.
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

package org.datatransferproject.datatransfer.imgur;

import java.io.IOException;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import okhttp3.OkHttpClient;
import org.datatransferproject.api.launcher.ExtensionContext;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.spi.cloud.storage.JobStore;
import org.datatransferproject.spi.transfer.extension.TransferExtension;
import org.datatransferproject.spi.transfer.provider.Exporter;
import org.datatransferproject.spi.transfer.provider.Importer;
import org.datatransferproject.datatransfer.imgur.photos.ImgurPhotosExporter;

/** Extension for transferring Imgur data */
public class ImgurTransferExtension implements TransferExtension {
  private static final String SERVICE_ID = "Imgur";
  public static final String BASE_URL = "https://api.imgur.com/3";

  private boolean initialized = false;

  private static final ImmutableList<String> SUPPORTED_DATA_TYPES = ImmutableList.of("PHOTOS");

  private ImgurPhotosExporter exporter;

  @Override
  public void initialize(ExtensionContext context) {
    Monitor monitor = context.getMonitor();
    if (initialized) {
      monitor.severe(() -> "ImgurTransferExtension is already initialized");
      return;
    }

    ObjectMapper mapper =
        new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    OkHttpClient client = context.getService(OkHttpClient.class);
    JobStore jobStore = context.getService(JobStore.class);

    exporter = new ImgurPhotosExporter(monitor, client, mapper, jobStore);

    initialized = true;
  }

  @Override
  public String getServiceId() {
    return SERVICE_ID;
  }

  @Override
  public Exporter<?, ?> getExporter(String transferDataType) {
    Preconditions.checkArgument(
        initialized, "ImgurTransferExtension is not initialized. Unable to get Exporter");
    Preconditions.checkArgument(
        SUPPORTED_DATA_TYPES.contains(transferDataType),
        "ImgurTransferExtension doesn't support " + transferDataType);
    return exporter;
  }

  @Override
  public Importer<?, ?> getImporter(String transferDataType) {
    Preconditions.checkArgument(
        initialized, "ImgurTransferExtension is not initialized. Unable to get Importer");
    Preconditions.checkArgument(false, "Imgur importer is not implemented yet");
    return null;
  }
}
