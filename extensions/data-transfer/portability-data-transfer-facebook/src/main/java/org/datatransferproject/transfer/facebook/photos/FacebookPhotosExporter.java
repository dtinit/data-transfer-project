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

package org.datatransferproject.transfer.facebook.photos;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.restfb.Connection;
import com.restfb.exception.FacebookGraphException;
import com.restfb.types.Album;
import com.restfb.types.Photo;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.ImageWriteException;
import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.common.ImageMetadata;
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;
import org.apache.commons.imaging.formats.jpeg.exif.ExifRewriter;
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata;
import org.apache.commons.imaging.formats.tiff.constants.ExifTagConstants;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputDirectory;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputSet;
import org.apache.commons.io.IOUtils;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.spi.cloud.storage.TemporaryPerJobDataStore;
import org.datatransferproject.spi.transfer.provider.ExportResult;
import org.datatransferproject.spi.transfer.provider.Exporter;
import org.datatransferproject.spi.transfer.types.ContinuationData;
import org.datatransferproject.spi.transfer.types.CopyExceptionWithFailureReason;
import org.datatransferproject.transfer.ImageStreamProvider;
import org.datatransferproject.types.common.ExportInformation;
import org.datatransferproject.types.common.PaginationData;
import org.datatransferproject.types.common.StringPaginationToken;
import org.datatransferproject.types.common.models.ContainerResource;
import org.datatransferproject.types.common.models.IdOnlyContainerResource;
import org.datatransferproject.types.common.models.photos.PhotoAlbum;
import org.datatransferproject.types.common.models.photos.PhotoModel;
import org.datatransferproject.types.common.models.photos.PhotosContainerResource;
import org.datatransferproject.types.transfer.auth.AppCredentials;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;

public class FacebookPhotosExporter
    implements Exporter<TokensAndUrlAuthData, PhotosContainerResource> {

  private static final String ALBUM_TOKEN_PREFIX = "album:";
  static final String PHOTO_TOKEN_PREFIX = "media:";
  private final Monitor monitor;
  private final TemporaryPerJobDataStore store;
  private final ImageStreamProvider imageStreamProvider;

  private AppCredentials appCredentials;
  private FacebookPhotosInterface photosInterface;
  private final SimpleDateFormat exifDateFormat = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");

  public FacebookPhotosExporter(
      AppCredentials appCredentials,
      Monitor monitor,
      TemporaryPerJobDataStore store) {
    this.appCredentials = appCredentials;
    this.monitor = monitor;
    this.store = store;
    imageStreamProvider = new ImageStreamProvider();
  }

  @VisibleForTesting
  FacebookPhotosExporter(
      AppCredentials appCredentials,
      FacebookPhotosInterface photosInterface,
      Monitor monitor,
      TemporaryPerJobDataStore store,
      ImageStreamProvider imageStreamProvider) {
    this.appCredentials = appCredentials;
    this.photosInterface = photosInterface;
    this.monitor = monitor;
    this.store = store;
    this.imageStreamProvider = imageStreamProvider;
  }

  @Override
  public ExportResult<PhotosContainerResource> export(
      UUID jobId, TokensAndUrlAuthData authData, Optional<ExportInformation> exportInformation)
      throws CopyExceptionWithFailureReason {
    Preconditions.checkNotNull(authData);

    if (!exportInformation.isPresent()) {
      // No export information if at the start of a bulk export
      // Start by getting the list of albums to export
      return exportAlbums(authData, Optional.empty());
    }

    StringPaginationToken paginationToken =
        (StringPaginationToken) exportInformation.get().getPaginationData();
    ContainerResource containerResource = exportInformation.get().getContainerResource();

    boolean containerResourcePresent = containerResource != null;
    boolean paginationDataPresent = paginationToken != null;

    if (!containerResourcePresent
        && paginationDataPresent
        && paginationToken.getToken().startsWith(ALBUM_TOKEN_PREFIX)) {
      // Continue exporting albums
      return exportAlbums(authData, Optional.of(paginationToken));
    } else if (containerResourcePresent && containerResource instanceof PhotosContainerResource) {
      // We have had albums specified from the front end so process them for import
      PhotosContainerResource photosContainerResource = (PhotosContainerResource) containerResource;
      Preconditions.checkNotNull(photosContainerResource.getAlbums());
      ContinuationData continuationData = new ContinuationData(null);
      for (PhotoAlbum album : photosContainerResource.getAlbums()) {
        continuationData.addContainerResource(new IdOnlyContainerResource(album.getId()));
      }
      return new ExportResult<>(
          ExportResult.ResultType.CONTINUE, photosContainerResource, continuationData);
    } else if (containerResourcePresent && containerResource instanceof IdOnlyContainerResource) {
      // Export photos
      return exportPhotos(
          jobId,
          authData,
          (IdOnlyContainerResource) containerResource,
          Optional.ofNullable(paginationToken));
    } else {
      throw new IllegalStateException(
          String.format(
              "Invalid state passed into FacebookPhotosExporter. ExportInformation: %s",
              exportInformation));
    }
  }

  private ExportResult<PhotosContainerResource> exportAlbums(
      TokensAndUrlAuthData authData, Optional<StringPaginationToken> paginationData)
      throws CopyExceptionWithFailureReason {
    Optional<String> paginationToken = stripTokenPrefix(paginationData, ALBUM_TOKEN_PREFIX);

    // Get albums
    Connection<Album> connection = getOrCreatePhotosInterface(authData).getAlbums(paginationToken);

    PaginationData nextPageData = null;
    String token = connection.getAfterCursor();
    if (!Strings.isNullOrEmpty(token)) {
      nextPageData = new StringPaginationToken(ALBUM_TOKEN_PREFIX + token);
    }
    ContinuationData continuationData = new ContinuationData(nextPageData);

    List<Album> albums = connection.getData();

    if (albums.isEmpty()) {
      return new ExportResult<>(ExportResult.ResultType.END, null, null);
    }

    ArrayList<PhotoAlbum> exportAlbums = new ArrayList<>();
    for (Album album : albums) {
      exportAlbums.add(new PhotoAlbum(album.getId(), album.getName(), album.getDescription()));
      continuationData.addContainerResource(new IdOnlyContainerResource(album.getId()));
    }

    return new ExportResult<>(
        ExportResult.ResultType.CONTINUE,
        new PhotosContainerResource(exportAlbums, null),
        continuationData);
  }

  private ExportResult<PhotosContainerResource> exportPhotos(
      UUID jobId,
      TokensAndUrlAuthData authData,
      IdOnlyContainerResource containerResource,
      Optional<StringPaginationToken> paginationData)
      throws CopyExceptionWithFailureReason {
    Optional<String> paginationToken = stripTokenPrefix(paginationData, PHOTO_TOKEN_PREFIX);

    String albumId = containerResource.getId();
    try {
      Connection<Photo> photoConnection =
          getOrCreatePhotosInterface(authData).getPhotos(albumId, paginationToken);
      List<Photo> photos = photoConnection.getData();

      if (photos.isEmpty()) {
        return new ExportResult<>(ExportResult.ResultType.END, null);
      }

      ArrayList<PhotoModel> exportPhotos = new ArrayList<>();
      for (Photo photo : photos) {
        final String url = photo.getImages().get(0).getSource();
        final String fbid = photo.getId();
        if (null == url || url.isEmpty()) {
          monitor.severe(() -> String.format("Source was missing or empty for photo %s", fbid));
          continue;
        }
        boolean photoWasGarbage;
        try {
          photoWasGarbage = modifyExifAndStorePhoto(jobId, photo, url, photo.getId());
        } catch (IOException e) {
          monitor.info(
              () -> String.format("Error while modifying exif or storing photo %s", fbid), e);
          photoWasGarbage = true;
        }
        if (photoWasGarbage) {
          continue;
        }
        exportPhotos.add(
            new PhotoModel(
                String.format("%s.jpg", photo.getId()),
                // We use the blindedPhotoId as the URL as the importer will fetch it from the temp
                // store and the url is too long for that.
                photo.getId(),
                photo.getName(),
                "image/jpg",
                photo.getId(),
                albumId,
                true,
                photo.getCreatedTime()));
      }

      String token = photoConnection.getAfterCursor();
      if (Strings.isNullOrEmpty(token)) {
        return new ExportResult<>(
            ExportResult.ResultType.END, new PhotosContainerResource(null, exportPhotos));
      } else {
        PaginationData nextPageData = new StringPaginationToken(PHOTO_TOKEN_PREFIX + token);
        ContinuationData continuationData = new ContinuationData(nextPageData);
        return new ExportResult<>(
            ExportResult.ResultType.CONTINUE,
            new PhotosContainerResource(null, exportPhotos),
            continuationData);
      }
    } catch (FacebookGraphException e) {
      String message = e.getMessage();
      // This error means the object we are trying to copy does not exist any more.
      // In such case, we should skip this object and continue with the rest of the transfer.
      if (message != null && message.contains("code 100, subcode 33")) {
        monitor.info(() -> "Cannot find photos to export, skipping to the next bunch", e);
        return new ExportResult<>(ExportResult.ResultType.END, null);
      }
      throw e;
    }
  }

  /**
   * This method fetches the image from the specified URL, modifies the EXIF to include the created
   * date, and then stores the modified photo via the store on the local filesystem.
   *
   * @param jobId Id for the current transfer
   * @param photo The photo model returned from the API
   * @param url The source url for the photo
   * @param blindedPhotoId The blinded Id we will share with the importer
   * @return True if we should skip this photo because it is empty
   * @throws IOException If there is an issue with modifying the exif data
   */
  private boolean modifyExifAndStorePhoto(
      UUID jobId, Photo photo, String url, String blindedPhotoId) throws IOException {
    try (InputStream inputStream = imageStreamProvider.getConnection(url).getInputStream()) {
      final byte[] bytes = IOUtils.toByteArray(inputStream);
      if (bytes.length == 0) {
        // We should not upload an empty photo and Google cannot handle it.
        return true;
      }

      if (null == photo.getCreatedTime()) {
        try (ByteArrayInputStream unmodifiedInputStream = new ByteArrayInputStream(bytes)) {
          store.create(jobId, blindedPhotoId, unmodifiedInputStream);
        }
        return false;
      }

      final ImageMetadata metadata = Imaging.getMetadata(bytes);
      final JpegImageMetadata jpegMetadata = (JpegImageMetadata) metadata;
      TiffOutputSet outputSet = null;
      if (null != jpegMetadata) {
        // note that exif might be null if no Exif metadata is found.
        final TiffImageMetadata exif = jpegMetadata.getExif();

        if (null != exif) {
          // TiffImageMetadata class is immutable (read-only).
          // TiffOutputSet class represents the Exif data to write.
          //
          // Usually, we want to update existing Exif metadata by
          // changing
          // the values of a few fields, or adding a field.
          // In these cases, it is easiest to use getOutputSet() to
          // start with a "copy" of the fields read from the image.
          outputSet = exif.getOutputSet();
        }
      }
      if (null == outputSet) {
        outputSet = new TiffOutputSet();
      }
      final TiffOutputDirectory exifDirectory = outputSet.getOrCreateExifDirectory();
      exifDirectory.removeField(ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL);
      exifDirectory.add(
          ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL,
          exifDateFormat.format(photo.getCreatedTime()));
      try (final ByteArrayOutputStream outputStream = new ByteArrayOutputStream(bytes.length)) {
        new ExifRewriter().updateExifMetadataLossless(bytes, outputStream, outputSet);
        try (ByteArrayInputStream modifiedInputStream =
            new ByteArrayInputStream(outputStream.toByteArray())) {
          store.create(jobId, blindedPhotoId, modifiedInputStream);
        }
      }
    } catch (ImageReadException | ImageWriteException e) {
      monitor.severe(
          () ->
              String.format("There was an issue when modifying the exif data of %s", photo.getId()),
          e);
      return true;
    }
    return false;
  }

  private Optional<String> stripTokenPrefix(
      Optional<StringPaginationToken> paginationData, String prefix) {
    Optional<String> paginationToken = Optional.empty();
    if (paginationData.isPresent()) {
      String token = paginationData.get().getToken();
      Preconditions.checkArgument(token.startsWith(prefix), "Invalid pagination token " + token);
      paginationToken = Optional.of(token.substring(prefix.length()));
    }
    return paginationToken;
  }

  private synchronized FacebookPhotosInterface getOrCreatePhotosInterface(
      TokensAndUrlAuthData authData) {
    return photosInterface == null ? makePhotosInterface(authData) : photosInterface;
  }

  private synchronized FacebookPhotosInterface makePhotosInterface(TokensAndUrlAuthData authData) {
    photosInterface = new RestFbFacebookPhotos(authData, appCredentials);
    return photosInterface;
  }
}
