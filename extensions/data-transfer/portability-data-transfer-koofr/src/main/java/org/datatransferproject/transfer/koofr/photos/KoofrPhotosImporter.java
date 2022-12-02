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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.atomic.LongAdder;

import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.common.ImageMetadata;
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata;
import org.apache.commons.imaging.formats.tiff.constants.ExifTagConstants;
import org.apache.commons.io.IOUtils;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.spi.cloud.connection.ConnectionProvider;
import org.datatransferproject.spi.cloud.storage.JobStore;
import org.datatransferproject.spi.cloud.storage.TemporaryPerJobDataStore.InputStreamWrapper;
import org.datatransferproject.spi.transfer.idempotentexecutor.IdempotentImportExecutor;
import org.datatransferproject.spi.transfer.idempotentexecutor.ItemImportResult;
import org.datatransferproject.spi.transfer.provider.ImportResult;
import org.datatransferproject.spi.transfer.provider.Importer;
import org.datatransferproject.spi.transfer.types.DestinationMemoryFullException;
import org.datatransferproject.spi.transfer.types.InvalidTokenException;
import org.datatransferproject.transfer.koofr.KoofrTransmogrificationConfig;
import org.datatransferproject.transfer.koofr.common.KoofrClient;
import org.datatransferproject.transfer.koofr.common.KoofrClientFactory;
import org.datatransferproject.transfer.koofr.exceptions.KoofrClientIOException;
import org.datatransferproject.types.common.models.photos.PhotoAlbum;
import org.datatransferproject.types.common.models.photos.PhotoModel;
import org.datatransferproject.types.common.models.photos.PhotosContainerResource;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;

import static java.lang.String.format;

/** Imports albums and photos to Koofr. */
public class KoofrPhotosImporter
    implements Importer<TokensAndUrlAuthData, PhotosContainerResource> {

  private static final String TITLE_DATE_FORMAT = "yyyy-MM-dd HH.mm.ss ";
  private final KoofrClientFactory koofrClientFactory;
  private final JobStore jobStore;
  private final ConnectionProvider connectionProvider;
  private final Monitor monitor;
  private final KoofrTransmogrificationConfig transmogrificationConfig =
      new KoofrTransmogrificationConfig();

  private final SimpleDateFormat exifDateFormat = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");

  private volatile HashMap<UUID, SimpleDateFormat> titleDateFormats = new HashMap<>();

  public KoofrPhotosImporter(
      KoofrClientFactory koofrClientFactory, Monitor monitor, JobStore jobStore) {
    this.koofrClientFactory = koofrClientFactory;
    this.connectionProvider = new ConnectionProvider(jobStore);
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
    KoofrClient koofrClient = koofrClientFactory.create(authData);
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
          album.getId(), album.getName(), () -> createAlbumFolder(album, koofrClient));
    }

    final LongAdder totalImportedFilesSizes = new LongAdder();
    for (PhotoModel photoModel : resource.getPhotos()) {
      idempotentImportExecutor.importAndSwallowIOExceptions(
          photoModel,
          photo -> {
            ItemImportResult<String> fileImportResult =
                    importSinglePhoto(photoModel, jobId, idempotentImportExecutor, koofrClient);
            if (fileImportResult.hasBytes()) {
              totalImportedFilesSizes.add(fileImportResult.getBytes());
            }
            return fileImportResult;
          });
    }
    return ImportResult.OK.copyWithBytes(totalImportedFilesSizes.longValue());
  }

  private String createAlbumFolder(PhotoAlbum album, KoofrClient koofrClient)
      throws IOException, InvalidTokenException {
    String albumName = KoofrTransmogrificationConfig.getAlbumName(album.getName());

    monitor.debug(() -> String.format("Create Koofr folder %s", albumName));

    String rootPath = koofrClient.ensureRootFolder();
    String fullPath = rootPath + "/" + albumName;

    koofrClient.ensureFolder(rootPath, albumName);

    String description = KoofrClient.trimDescription(album.getDescription());

    if (description != null && description.length() > 0) {
      koofrClient.addDescription(fullPath, description);
    }

    return fullPath;
  }

  private ItemImportResult<String> importSinglePhoto(
      PhotoModel photo,
      UUID jobId,
      IdempotentImportExecutor idempotentImportExecutor,
      KoofrClient koofrClient)
          throws IOException, InvalidTokenException, DestinationMemoryFullException {
    monitor.debug(() -> String.format("Import single photo %s", photo.getTitle()));
    Long size = null;
    try {
      InputStreamWrapper inputStreamWrapper =
              connectionProvider.getInputStreamForItem(jobId, photo);
      ItemImportResult<String> response;

      try (InputStream inputStream = inputStreamWrapper.getStream()) {
        final byte[] bytes = IOUtils.toByteArray(inputStream);

        Date dateCreated = getDateCreated(photo, bytes);

        String title = buildPhotoTitle(jobId, photo.getTitle(), dateCreated);
        String description = KoofrClient.trimDescription(photo.getDescription());

        String parentPath = idempotentImportExecutor.getCachedValue(photo.getAlbumId());
        String fullPath = parentPath + "/" + title;

        if (koofrClient.fileExists(fullPath)) {
          monitor.debug(() -> String.format("Photo already exists %s", photo.getTitle()));

          return ItemImportResult.success(fullPath);
        }

        final ByteArrayInputStream inMemoryInputStream = new ByteArrayInputStream(bytes);

        try {
          long inputStreamBytes = inputStreamWrapper.getBytes();
          String stringResult = koofrClient.uploadFile(
                  parentPath, title, inMemoryInputStream, photo.getMediaType(), dateCreated, description);
          if (stringResult != null && !stringResult.isEmpty()) {
            response = ItemImportResult.success(
                    stringResult,
                    inputStreamBytes);
          } else {
            response = ItemImportResult.success("skipped-" + photo.getDataId());
          }
          size = inputStreamBytes;
        } catch (KoofrClientIOException exception) {
          if (exception.getCode() == 404) {
            monitor.info(() -> String.format("Can't find album during importSingleItem for id: %s", photo.getDataId()), exception);
            response = ItemImportResult.success("skipped-" + photo.getDataId());
          } else {
            Long finalSize = size;
            return ItemImportResult.error(exception, finalSize);
          }
        }

        try {
          if (photo.isInTempStore()) {
            jobStore.removeData(jobId, photo.getFetchableUrl());
          }
        } catch (Exception e) {
          // Swallow the exception caused by Remove data so that existing flows continue
          monitor.info(
                  () -> format("Exception swallowed while removing data for jobId %s, localPath %s",
                          jobId, photo.getFetchableUrl()), e);
        }
      }

      return response;
    } catch (KoofrClientIOException exception) {
      Long finalSize = size;
      return ItemImportResult.error(exception, finalSize);
    }
  }

  private String buildPhotoTitle(UUID jobId, String originalTitle, Date dateCreated) {
    if (dateCreated == null) {
      return originalTitle;
    }

    SimpleDateFormat dateFormat = getOrCreateTitleDateFormat(jobId);
    return dateFormat.format(dateCreated) + originalTitle;
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

  private synchronized SimpleDateFormat getOrCreateTitleDateFormat(UUID jobId) {
    if (titleDateFormats.containsKey(jobId)) {
      return titleDateFormats.get(jobId);
    }

    SimpleDateFormat dateFormat = new SimpleDateFormat(TITLE_DATE_FORMAT);
    TimeZone userTimeZone = jobStore.findJob(jobId).userTimeZone();
    if (null != userTimeZone) {
      dateFormat.setTimeZone(userTimeZone);
    }

    titleDateFormats.put(jobId, dateFormat);

    return dateFormat;
  }
}
