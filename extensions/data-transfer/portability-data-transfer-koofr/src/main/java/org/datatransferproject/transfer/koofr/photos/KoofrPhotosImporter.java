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

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;
import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.common.ImageMetadata;
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata;
import org.apache.commons.imaging.formats.tiff.constants.ExifTagConstants;
import org.apache.commons.io.IOUtils;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.spi.cloud.storage.TemporaryPerJobDataStore;
import org.datatransferproject.spi.transfer.idempotentexecutor.IdempotentImportExecutor;
import org.datatransferproject.spi.transfer.provider.ImportResult;
import org.datatransferproject.spi.transfer.provider.Importer;
import org.datatransferproject.transfer.ImageStreamProvider;
import org.datatransferproject.transfer.koofr.KoofrTransmogrificationConfig;
import org.datatransferproject.transfer.koofr.common.KoofrClient;
import org.datatransferproject.types.common.models.photos.PhotoAlbum;
import org.datatransferproject.types.common.models.photos.PhotoModel;
import org.datatransferproject.types.common.models.photos.PhotosContainerResource;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;

/** Imports albums and photos to Koofr. */
public class KoofrPhotosImporter
    implements Importer<TokensAndUrlAuthData, PhotosContainerResource> {

  private final KoofrClient koofrClient;
  private final TemporaryPerJobDataStore jobStore;
  private final ImageStreamProvider imageStreamProvider;
  private final Monitor monitor;
  private final KoofrTransmogrificationConfig transmogrificationConfig =
      new KoofrTransmogrificationConfig();

  private final SimpleDateFormat exifDateFormat = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");
  private final SimpleDateFormat titleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH.mm.ss ");

  public KoofrPhotosImporter(
      KoofrClient koofrClient, Monitor monitor, TemporaryPerJobDataStore jobStore) {
    this.koofrClient = koofrClient;
    this.imageStreamProvider = new ImageStreamProvider();
    this.monitor = monitor;
    this.jobStore = jobStore;
  }

  @Override
  public ImportResult importItem(
      UUID jobId,
      IdempotentImportExecutor idempotentImportExecutor,
      TokensAndUrlAuthData authData,
      PhotosContainerResource resource)
      throws Exception {
    // Ensure credential is populated
    koofrClient.getOrCreateCredential(authData);

    monitor.debug(
        () ->
            String.format(
                "%s: Importing %s albums and %s photos before transmogrification",
                jobId, resource.getAlbums().size(), resource.getPhotos().size()));

    // Make the data Koofr compatible
    resource.transmogrify(transmogrificationConfig);
    monitor.debug(
        () ->
            String.format(
                "%s: Importing %s albums and %s photos after transmogrification",
                jobId, resource.getAlbums().size(), resource.getPhotos().size()));

    for (PhotoAlbum album : resource.getAlbums()) {
      // Create a Koofr folder and then save the id with the mapping data
      idempotentImportExecutor.executeAndSwallowIOExceptions(
          album.getId(), album.getName(), () -> createAlbumFolder(album));
    }

    for (PhotoModel photoModel : resource.getPhotos()) {
      idempotentImportExecutor.executeAndSwallowIOExceptions(
          photoModel.getAlbumId() + "-" + photoModel.getDataId(),
          photoModel.getTitle(),
          () -> importSinglePhoto(photoModel, jobId, idempotentImportExecutor));
    }
    return ImportResult.OK;
  }

  private String createAlbumFolder(PhotoAlbum album) throws IOException {
    monitor.debug(() -> String.format("Create Koofr folder %s", album.getName()));

    String rootPath = koofrClient.ensureRootFolder();
    String fullPath = rootPath + "/" + album.getName();

    koofrClient.ensureFolder(rootPath, album.getName());

    String description = KoofrClient.trimDescription(album.getDescription());

    if (description != null && description.length() > 0) {
      koofrClient.addDescription(fullPath, description);
    }

    return fullPath;
  }

  private String importSinglePhoto(
      PhotoModel photo, UUID jobId, IdempotentImportExecutor idempotentImportExecutor)
      throws Exception {
    monitor.debug(() -> String.format("Import single photo %s", photo.getTitle()));

    BufferedInputStream inputStream = null;
    try {
      if (photo.isInTempStore()) {
        inputStream =
            new BufferedInputStream(jobStore.getStream(jobId, photo.getFetchableUrl()).getStream());
      } else if (photo.getFetchableUrl() != null) {
        HttpURLConnection conn = imageStreamProvider.getConnection(photo.getFetchableUrl());
        inputStream = new BufferedInputStream(conn.getInputStream());
      } else {
        throw new IllegalStateException(
            "Don't know how to get the inputStream for " + photo.getTitle());
      }

      final byte[] bytes = IOUtils.toByteArray(inputStream);

      Date dateCreated = getDateCreated(photo, bytes);

      String title = buildPhotoTitle(photo.getTitle(), dateCreated);
      String description = KoofrClient.trimDescription(photo.getDescription());

      String parentPath = idempotentImportExecutor.getCachedValue(photo.getAlbumId());
      String fullPath = parentPath + "/" + title;

      if (koofrClient.fileExists(fullPath)) {
        monitor.debug(() -> String.format("Photo already exists %s", photo.getTitle()));

        return fullPath;
      }

      final ByteArrayInputStream inMemoryInputStream = new ByteArrayInputStream(bytes);

      return koofrClient.uploadFile(
          parentPath, title, inMemoryInputStream, photo.getMediaType(), dateCreated, description);
    } finally {
      if (inputStream != null) {
        inputStream.close();
      }
    }
  }

  private String buildPhotoTitle(String originalTitle, Date dateCreated) {
    if (dateCreated == null) {
      return originalTitle;
    }

    return titleDateFormat.format(dateCreated) + originalTitle;
  }

  private Date getDateCreated(PhotoModel photo, byte[] bytes) {
    if (photo.getUploadedTime() != null) {
      return photo.getUploadedTime();
    }

    try {
      final ImageMetadata metadata = Imaging.getMetadata(bytes);

      if (metadata == null) {
        return null;
      }

      final JpegImageMetadata jpegMetadata = (JpegImageMetadata) metadata;

      final TiffImageMetadata exif = jpegMetadata.getExif();

      if (exif == null) {
        return null;
      }

      String[] values = exif.getFieldValue(ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL);

      if (values == null || values.length == 0) {
        values = exif.getFieldValue(ExifTagConstants.EXIF_TAG_DATE_TIME_DIGITIZED);
      }

      if (values == null || values.length == 0) {
        return null;
      }

      return exifDateFormat.parse(values[0]);
    } catch (Exception e) {
      monitor.info(
          () ->
              String.format(
                  "There was an issue when reading the exif data of %s", photo.getDataId()),
          e);
      return null;
    }
  }
}
