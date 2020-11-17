/*
 * Copyright 2020 The Data-Portability Project Authors.
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
package org.datatransferproject.transfer.koofr.photos;

import com.google.common.base.Preconditions;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.spi.transfer.provider.ExportResult;
import org.datatransferproject.spi.transfer.provider.Exporter;
import org.datatransferproject.spi.transfer.types.CopyExceptionWithFailureReason;
import org.datatransferproject.transfer.koofr.common.KoofrClient;
import org.datatransferproject.transfer.koofr.common.KoofrClientFactory;
import org.datatransferproject.transfer.koofr.common.KoofrMediaExport;
import org.datatransferproject.types.common.ExportInformation;
import org.datatransferproject.types.common.models.photos.PhotoAlbum;
import org.datatransferproject.types.common.models.photos.PhotoModel;
import org.datatransferproject.types.common.models.photos.PhotosContainerResource;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;

public class KoofrPhotosExporter
    implements Exporter<TokensAndUrlAuthData, PhotosContainerResource> {

  private final Monitor monitor;

  private final KoofrClientFactory koofrClientFactory;

  public KoofrPhotosExporter(KoofrClientFactory koofrClientFactory, Monitor monitor) {
    this.koofrClientFactory = koofrClientFactory;
    this.monitor = monitor;
  }

  @Override
  public ExportResult<PhotosContainerResource> export(
      UUID jobId, TokensAndUrlAuthData authData, Optional<ExportInformation> exportInformation)
      throws CopyExceptionWithFailureReason {
    Preconditions.checkNotNull(authData);

    KoofrClient koofrClient = koofrClientFactory.create(authData);
    KoofrMediaExport export = new KoofrMediaExport(koofrClient, monitor);

    try {
      export.export();

      List<PhotoAlbum> exportAlbums = export.getPhotoAlbums();
      List<PhotoModel> exportPhotos = export.getPhotos();

      PhotosContainerResource containerResource =
          new PhotosContainerResource(exportAlbums, exportPhotos);

      return new ExportResult<>(ExportResult.ResultType.END, containerResource, null);
    } catch (IOException e) {
      return new ExportResult<>(e);
    }
  }
}
