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

import static org.datatransferproject.types.common.models.DataVertical.PHOTOS;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.util.Optional;
import okhttp3.OkHttpClient;
import org.datatransferproject.api.launcher.ExtensionContext;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.datatransfer.imgur.photos.ImgurPhotosExporter;
import org.datatransferproject.datatransfer.imgur.photos.ImgurPhotosImporter;
import org.datatransferproject.spi.cloud.storage.TemporaryPerJobDataStore;
import org.datatransferproject.types.common.models.DataVertical;
import org.datatransferproject.spi.transfer.extension.TransferExtension;
import org.datatransferproject.spi.transfer.provider.Exporter;
import org.datatransferproject.spi.transfer.provider.Importer;
import org.datatransferproject.types.transfer.serviceconfig.TransferServiceConfig;

/** Extension for transferring Imgur data */
public class ImgurTransferExtension implements TransferExtension {
  private static final String SERVICE_ID = "Imgur";

  @VisibleForTesting
  static final String DEFAULT_BASE_URL = "https://api.imgur.com/3";

  private boolean initialized = false;

  private static final ImmutableList<DataVertical> SUPPORTED_DATA_TYPES = ImmutableList.of(PHOTOS);

  private ImgurPhotosExporter exporter;
  private ImgurPhotosImporter importer;

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
    TemporaryPerJobDataStore jobStore = context.getService(TemporaryPerJobDataStore.class);

    String baseUrl = baseUrl(context.getService(TransferServiceConfig.class));

    exporter = new ImgurPhotosExporter(monitor, client, mapper, jobStore, baseUrl);
    importer = new ImgurPhotosImporter(monitor, client, mapper, jobStore, baseUrl);

    initialized = true;
  }

  /**
   * The API root, from {@code config/imgur.yaml} if one is on the classpath.
   *
   * <p>Follows the convention Flickr and Deezer already use, so a deployer can point the adapter at
   * a staging endpoint or a test double without rebuilding. Defaults to Imgur's own.
   */
  @VisibleForTesting
  static String baseUrl(TransferServiceConfig serviceConfig) {
    Optional<JsonNode> config = serviceConfig.getServiceConfig();
    return config
        .map(node -> node.path("baseUrl").asText(DEFAULT_BASE_URL))
        .orElse(DEFAULT_BASE_URL);
  }

  @Override
  public String getServiceId() {
    return SERVICE_ID;
  }

  @Override
  public Exporter<?, ?> getExporter(DataVertical transferDataType) {
    Preconditions.checkArgument(
        initialized, "ImgurTransferExtension is not initialized. Unable to get Exporter");
    Preconditions.checkArgument(
        SUPPORTED_DATA_TYPES.contains(transferDataType),
        "ImgurTransferExtension doesn't support " + transferDataType);
    return exporter;
  }

  @Override
  public Importer<?, ?> getImporter(DataVertical transferDataType) {
    Preconditions.checkArgument(
        initialized, "ImgurTransferExtension is not initialized. Unable to get Importer");
    Preconditions.checkArgument(
        SUPPORTED_DATA_TYPES.contains(transferDataType),
        "ImgurTransferExtension doesn't support " + transferDataType);
    return importer;
  }
}
