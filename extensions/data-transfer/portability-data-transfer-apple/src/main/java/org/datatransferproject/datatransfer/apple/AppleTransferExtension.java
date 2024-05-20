/*
 * Copyright 2023 The Data Transfer Project Authors.
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

package org.datatransferproject.datatransfer.apple;

import static org.datatransferproject.datatransfer.apple.constants.AppleConstants.APPLE_KEY;
import static org.datatransferproject.datatransfer.apple.constants.AppleConstants.APPLE_SECRET;
import static org.datatransferproject.datatransfer.apple.constants.AppleConstants.DTP_SERVICE_ID;
import static org.datatransferproject.types.common.models.DataVertical.MEDIA;
import static org.datatransferproject.types.common.models.DataVertical.MUSIC;
import static org.datatransferproject.types.common.models.DataVertical.PHOTOS;
import static org.datatransferproject.types.common.models.DataVertical.VIDEOS;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.datatransferproject.api.launcher.ExtensionContext;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.datatransfer.apple.photos.AppleMediaImporter;
import org.datatransferproject.datatransfer.apple.photos.ApplePhotosImporter;
import org.datatransferproject.datatransfer.apple.photos.AppleVideosImporter;
import org.datatransferproject.datatransfer.apple.music.AppleMusicImporter;
import org.datatransferproject.spi.cloud.storage.AppCredentialStore;
import org.datatransferproject.spi.transfer.extension.TransferExtension;
import org.datatransferproject.spi.transfer.provider.Exporter;
import org.datatransferproject.spi.transfer.provider.Importer;
import org.datatransferproject.types.common.models.DataVertical;
import org.datatransferproject.types.transfer.auth.AppCredentials;

/** TransferExtension to kick off the Transfer jobs to/from Apple */
public class AppleTransferExtension implements TransferExtension {

  private boolean initialized = false;

  private ImmutableMap<DataVertical, Importer> importerMap;
  private ImmutableMap<DataVertical, Exporter> exporterMap;

  private static final ImmutableList<DataVertical> SUPPORTED_SERVICES =
      ImmutableList.of(PHOTOS, VIDEOS, MEDIA, MUSIC);

  @Override
  public String getServiceId() {
    return DTP_SERVICE_ID;
  }

  @Override
  public Exporter<?, ?> getExporter(DataVertical transferDataType) {
    Preconditions.checkArgument(initialized);
    Preconditions.checkArgument(SUPPORTED_SERVICES.contains(transferDataType));
    return exporterMap.get(transferDataType);
  }

  @Override
  public Importer<?, ?> getImporter(DataVertical transferDataType) {
    Preconditions.checkArgument(initialized);
    Preconditions.checkArgument(SUPPORTED_SERVICES.contains(transferDataType));
    return importerMap.get(transferDataType);
  }

  @Override
  public void initialize(ExtensionContext context) {
    if (initialized) return;

    Monitor monitor = context.getMonitor();

    AppCredentials appCredentials;
    try {
      appCredentials =
          context.getService(AppCredentialStore.class).getAppCredentials(APPLE_KEY, APPLE_SECRET);

    } catch (Exception e) {
      monitor.info(
          () ->
              "Unable to retrieve Apple AppCredentials. Please configure APPLE_KEY and APPLE_SECRET.");
      return;
    }

    importerMap =
        ImmutableMap.<DataVertical, Importer>builder()
            .put(PHOTOS, new ApplePhotosImporter(appCredentials, monitor))
            .put(VIDEOS, new AppleVideosImporter(appCredentials, monitor))
            .put(MEDIA, new AppleMediaImporter(appCredentials, monitor))
            .put(MUSIC, new AppleMusicImporter(appCredentials, monitor))
            .build();

    exporterMap = ImmutableMap.<DataVertical, Exporter>builder().build();

    initialized = true;
  }
}
