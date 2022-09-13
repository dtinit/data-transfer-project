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

package org.datatransferproject.transfer.daybook;

import static org.datatransferproject.types.common.models.DataVertical.PHOTOS;
import static org.datatransferproject.types.common.models.DataVertical.SOCIAL_POSTS;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import okhttp3.OkHttpClient;
import org.datatransferproject.api.launcher.ExtensionContext;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.spi.cloud.storage.TemporaryPerJobDataStore;
import org.datatransferproject.types.common.models.DataVertical;
import org.datatransferproject.spi.transfer.extension.TransferExtension;
import org.datatransferproject.spi.transfer.provider.Exporter;
import org.datatransferproject.spi.transfer.provider.Importer;
import org.datatransferproject.transfer.JobMetadata;
import org.datatransferproject.transfer.daybook.photos.DaybookPhotosImporter;
import org.datatransferproject.transfer.daybook.social.DaybookPostsImporter;

/** Extension for transferring Daybook data */
public class DaybookTransferExtension implements TransferExtension {
  private static final String SERVICE_ID = "Daybook";
  private static final String BASE_URL =
      "https://us-central1-diary-a77f6.cloudfunctions.net/post-daybook-dtp";
  private static final ImmutableList<DataVertical> SUPPORTED_DATA_TYPES =
      ImmutableList.of(PHOTOS, SOCIAL_POSTS);

  private boolean initialized = false;
  private ImmutableMap<DataVertical, Importer<?, ?>> importerMap;

  @Override
  public void initialize(ExtensionContext context) {
    Monitor monitor = context.getMonitor();
    if (initialized) {
      monitor.severe(() -> "DaybookTransferExtension is already initialized");
      return;
    }

    ObjectMapper mapper =
        new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    OkHttpClient client = context.getService(OkHttpClient.class);
    TemporaryPerJobDataStore jobStore = context.getService(TemporaryPerJobDataStore.class);

    ImmutableMap.Builder<DataVertical, Importer<?, ?>> importerBuilder = ImmutableMap.builder();
    String exportService = JobMetadata.getExportService();
    importerBuilder.put(
        PHOTOS, new DaybookPhotosImporter(monitor, client, jobStore, BASE_URL, exportService));
    importerBuilder.put(
        SOCIAL_POSTS, new DaybookPostsImporter(monitor, client, mapper, BASE_URL, exportService));
    importerMap = importerBuilder.build();
    initialized = true;
  }

  @Override
  public String getServiceId() {
    return SERVICE_ID;
  }

  @Override
  public Importer<?, ?> getImporter(DataVertical transferDataType) {
    Preconditions.checkArgument(
        initialized, "DaybookTransferExtension is not initialized. Unable to get Importer");
    Preconditions.checkArgument(
        SUPPORTED_DATA_TYPES.contains(transferDataType),
        "DaybookTransferExtension doesn't support " + transferDataType);
    return importerMap.get(transferDataType);
  }

  @Override
  public Exporter<?, ?> getExporter(DataVertical transferDataType) {
    throw new IllegalArgumentException();
  }
}
