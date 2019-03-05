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

package org.datatransferproject.datatransfer.imgur.photos;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.datatransfer.imgur.ImgurTransferExtension;
import org.datatransferproject.spi.cloud.storage.JobStore;
import org.datatransferproject.spi.transfer.provider.ExportResult;
import org.datatransferproject.spi.transfer.provider.Exporter;
import org.datatransferproject.spi.transfer.types.ContinuationData;
import org.datatransferproject.types.common.ExportInformation;
import org.datatransferproject.types.common.IntPaginationToken;
import org.datatransferproject.types.common.PaginationData;
import org.datatransferproject.types.common.models.IdOnlyContainerResource;
import org.datatransferproject.types.common.models.photos.PhotoAlbum;
import org.datatransferproject.types.common.models.photos.PhotoModel;
import org.datatransferproject.types.common.models.photos.PhotosContainerResource;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

import static java.lang.String.format;

/** Exports Imgur albums and photos using Imgur API */
public class ImgurPhotosExporter
    implements Exporter<TokensAndUrlAuthData, PhotosContainerResource> {
  private static final String BASE_URL = ImgurTransferExtension.BASE_URL;
  private static final String RESULTS_PER_PAGE = "10";
  private static final String ALBUM_PHOTOS_URL_TEMPLATE = BASE_URL + "/album/%s/images";
  private static final String ALBUMS_URL_TEMPLATE =
      BASE_URL + "/account/me/albums/%s?perPage=" + RESULTS_PER_PAGE;
  private static final String ALL_PHOTOS_URL_TEMPLATE =
      BASE_URL + "/account/me/images/%s?perPage=" + RESULTS_PER_PAGE;
  private static final String DEFAULT_ALBUM_ID = "defaultAlbumId";
  private boolean containsNonAlbumPhotos = false;
  private Set<String> albumPhotos = new HashSet<>();

  private final OkHttpClient client;
  private final ObjectMapper objectMapper;
  private final Monitor monitor;
  private final JobStore jobStore;

  public ImgurPhotosExporter(
      Monitor monitor, OkHttpClient client, ObjectMapper objectMapper, JobStore jobStore) {
    this.client = client;
    this.objectMapper = objectMapper;
    this.monitor = monitor;
    this.jobStore = jobStore;
  }

  /**
   * Exports albums and photos. Gets albums first, then photos which are contained in albums and
   * non-album photos
   *
   * @param jobId the job id
   * @param authData authentication data for the operation
   * @param exportInformation info about what data to export, see {@link ExportInformation} for more
   */
  @Override
  public ExportResult<PhotosContainerResource> export(
      UUID jobId, TokensAndUrlAuthData authData, Optional<ExportInformation> exportInformation)
      throws Exception {

    PaginationData paginationData =
        exportInformation.isPresent() ? exportInformation.get().getPaginationData() : null;
    IdOnlyContainerResource resource =
        exportInformation.isPresent()
            ? (IdOnlyContainerResource) exportInformation.get().getContainerResource()
            : null;
    if (resource != null) {
      return requestPhotos(authData, resource, paginationData, jobId);
    } else {
      return requestAlbums(authData, paginationData);
    }
  }

  /**
   * Exports albums.
   *
   * @param authData authentication information
   * @param paginationData pagination information to use for subsequent calls
   */
  private ExportResult<PhotosContainerResource> requestAlbums(
      TokensAndUrlAuthData authData, PaginationData paginationData) throws IOException {
    ImmutableList.Builder<PhotoAlbum> albumBuilder = ImmutableList.builder();
    List<IdOnlyContainerResource> albumIds = new ArrayList<>();

    int page = paginationData == null ? 0 : ((IntPaginationToken) paginationData).getStart();

    String url = format(ALBUMS_URL_TEMPLATE, page);

    List<Map<String, Object>> items = requestData(authData, url);

    // Request result doesn't indicate if it's the last page
    boolean hasMore = (items != null && items.size() != 0);

    for (Map<String, Object> item : items) {
      albumBuilder.add(
          new PhotoAlbum(
              (String) item.get("id"),
              (String) item.get("title"),
              (String) item.get("description")));
      // Save album id for recalling export to get all the photos in albums
      albumIds.add(new IdOnlyContainerResource((String) item.get("id")));
    }

    if (page == 0) {
      // For checking non-album photos. Their export should be performed after all the others
      // Album will be created later
      albumIds.add(new IdOnlyContainerResource(DEFAULT_ALBUM_ID));
    }

    PaginationData newPage = null;
    if (hasMore) {
      newPage = new IntPaginationToken(page + 1);
      int start = ((IntPaginationToken) newPage).getStart();
      monitor.info(() -> format("albums size: %s, newPage: %s", items.size(), start));
    }

    PhotosContainerResource photosContainerResource =
        new PhotosContainerResource(albumBuilder.build(), null);

    ContinuationData continuationData = new ContinuationData(newPage);
    albumIds.forEach(continuationData::addContainerResource);

    ExportResult.ResultType resultType = ExportResult.ResultType.CONTINUE;
    if (newPage == null) {
      resultType = ExportResult.ResultType.END;
    }
    return new ExportResult<>(resultType, photosContainerResource, continuationData);
  }

  /**
   * Exports photos from album.
   *
   * <p>This request doesn't support pages so it retrieves all photos at once for each album.
   *
   * @param authData authentication information
   * @param resource contains album id
   * @param paginationData pagination information to use for subsequent calls.
   */
  private ExportResult<PhotosContainerResource> requestPhotos(
      TokensAndUrlAuthData authData,
      IdOnlyContainerResource resource,
      PaginationData paginationData,
      UUID jobId)
      throws IOException {
    String albumId = resource.getId();

    // Means all other albums with photos are imported, so non-album photos can be determined
    if (albumId.equals(DEFAULT_ALBUM_ID)) {
      return requestNonAlbumPhotos(authData, paginationData, jobId);
    }

    String url = format(ALBUM_PHOTOS_URL_TEMPLATE, albumId);
    List<PhotoModel> photos = new ArrayList<>();

    List<Map<String, Object>> items = requestData(authData, url);
    for (Map<String, Object> item : items) {
      PhotoModel photoModel =
          new PhotoModel(
              (String) item.get("name"),
              (String) item.get("link"),
              (String) item.get("description"),
              (String) item.get("type"),
              (String) item.get("id"),
              albumId,
              true);
      photos.add(photoModel);

      InputStream inputStream = getImageAsStream(photoModel.getFetchableUrl());
      jobStore.create(jobId, photoModel.getFetchableUrl(), inputStream);

      // Save id of each album photo for finding non-album photos later
      albumPhotos.add((String) item.get("id"));
    }

    // This request doesn't support pages
    ExportResult.ResultType resultType = ExportResult.ResultType.END;

    if (photos.size() > 0) {
      monitor.info(() -> format("added albumPhotos, size: %s", photos.size()));
    }

    PhotosContainerResource photosContainerResource = new PhotosContainerResource(null, photos);
    return new ExportResult<>(resultType, photosContainerResource, new ContinuationData(null));
  }

  /**
   * Queries all photos for the account. Chooses photos which are not included to the collection of
   * photos from albums.
   *
   * @param authData authentication information
   * @param paginationData pagination information to use for subsequent calls.
   */
  private ExportResult<PhotosContainerResource> requestNonAlbumPhotos(
      TokensAndUrlAuthData authData, PaginationData paginationData, UUID jobId) throws IOException {

    int page = paginationData == null ? 0 : ((IntPaginationToken) paginationData).getStart();

    String url = format(ALL_PHOTOS_URL_TEMPLATE, page);
    Set<PhotoAlbum> albums = new HashSet<>();
    List<PhotoModel> photos = new ArrayList<>();

    List<Map<String, Object>> items = requestData(authData, url);

    boolean hasMore = (items != null && items.size() != 0);

    for (Map<String, Object> item : items) {
      String photoId = (String) item.get("id");

      // Select photos which are not included to the collection of retrieved album photos
      if (!albumPhotos.contains(photoId)) {
        PhotoModel photoModel =
            new PhotoModel(
                (String) item.get("name"),
                (String) item.get("link"),
                (String) item.get("description"),
                (String) item.get("type"),
                (String) item.get("id"),
                DEFAULT_ALBUM_ID,
                true);
        photos.add(photoModel);

        InputStream inputStream = getImageAsStream(photoModel.getFetchableUrl());
        jobStore.create(jobId, photoModel.getFetchableUrl(), inputStream);
      }
    }

    if (!containsNonAlbumPhotos && photos.size() > 0) {
      // Add album for non-album photos
      albums.add(new PhotoAlbum(DEFAULT_ALBUM_ID, "Non-album photos", "Contains non-album photos"));
      // Make sure album will not be added multiply times on subsequent calls
      containsNonAlbumPhotos = true;
    }

    PaginationData newPage = null;
    if (hasMore) {
      newPage = new IntPaginationToken(page + 1);
      monitor.info(() -> format("added non-album photos, size: %s", photos.size()));
    }

    PhotosContainerResource photosContainerResource = new PhotosContainerResource(albums, photos);
    ContinuationData continuationData = new ContinuationData(newPage);

    ExportResult.ResultType resultType = ExportResult.ResultType.CONTINUE;
    if (newPage == null) {
      resultType = ExportResult.ResultType.END;
    }
    return new ExportResult<>(resultType, photosContainerResource, continuationData);
  }

  /**
   * Performs request to the given endpoint and returns received data
   *
   * @param authData authentication data for the operation
   * @param url query endpoint
   * @return collection of data
   */
  private List<Map<String, Object>> requestData(TokensAndUrlAuthData authData, String url)
      throws IOException {
    Request.Builder requestBuilder = new Request.Builder().url(url);
    requestBuilder.header("Authorization", "Bearer " + authData.getAccessToken());
    try (Response response = client.newCall(requestBuilder.build()).execute()) {
      ResponseBody body = response.body();
      if (body == null) {
        return null;
      }
      String contentBody = new String(body.bytes());
      Map contentMap = objectMapper.reader().forType(Map.class).readValue(contentBody);
      return (List<Map<String, Object>>) contentMap.get("data");
    }
  }

  private InputStream getImageAsStream(String imageUrl) throws IOException {
    URL url = new URL(imageUrl);
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    conn.connect();
    return conn.getInputStream();
  }
}
