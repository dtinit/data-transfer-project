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
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.spi.transfer.provider.ExportResult;
import org.datatransferproject.spi.transfer.provider.Exporter;
import org.datatransferproject.types.common.ExportInformation;

import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;
import org.datatransferproject.types.common.models.photos.PhotoAlbum;
import org.datatransferproject.types.common.models.photos.PhotoModel;
import org.datatransferproject.types.common.models.photos.PhotosContainerResource;

import java.io.IOException;
import java.util.*;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/** Exports Imgur albums and photos using Imgur API */
public class ImgurPhotosExporter
    implements Exporter<TokensAndUrlAuthData, PhotosContainerResource> {
  private final String albumPhotosUrlTemplate;
  private final String allPhotosUrlTemplate;
  private final String albumsUrlTemplate;
  private final String defaultAlbumId;

  private final OkHttpClient client;
  private final ObjectMapper objectMapper;
  private Monitor monitor;

  public ImgurPhotosExporter(Monitor monitor, String baseUrl) {
    albumPhotosUrlTemplate = baseUrl + "/album/%s/images";
    albumsUrlTemplate = baseUrl + "/account/me/albums/%s";
    allPhotosUrlTemplate = baseUrl + "/account/me/images/%s";
    defaultAlbumId = "0";
    this.client = new OkHttpClient.Builder().build();
    this.objectMapper = new ObjectMapper();
    this.monitor = monitor;
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
    HashMap photoItems = new HashMap<String, Object>();
    HashMap nonAlbumPhotoItems = new HashMap<String, Object>();

    Set<PhotoAlbum> albums = getAlbums(albumsUrlTemplate, authData);

    requestAlbumPhotos(authData, albums, photoItems);
    requestNonAlbumPhotos(authData, nonAlbumPhotoItems, photoItems);

    // include non-album photos to the result and create a new album for them
    if (nonAlbumPhotoItems.size() > 0) {
      albums.add(new PhotoAlbum(defaultAlbumId, "Non-album photos", "Contains non-album photos"));
      photoItems.putAll(nonAlbumPhotoItems);
    }

    List<PhotoModel> photoModels = convertPhotoItems(photoItems);
    monitor.info(() -> "albums size: %s, photoModels size: %s", albums.size(), photoModels.size());

    return new ExportResult<>(
        ExportResult.ResultType.END, new PhotosContainerResource(albums, photoModels));
  }

  /**
   * Queries for the albums. Iterates through pages until all albums are retrieved.
   *
   * @param urlTemplate url for the request
   * @param authData authentication information
   * @return albums associated with the account
   */
  private Set<PhotoAlbum> getAlbums(String urlTemplate, TokensAndUrlAuthData authData)
      throws IOException {
    Set<PhotoAlbum> albums = new HashSet<>();
    int page = 0;
    // continue processing until there are no albums returned for the next page
    while (true) {
      String url = String.format(urlTemplate, page);
      List<Map<String, Object>> items = requestData(authData, url);

      if (items == null || items.size() == 0) {
        return albums;
      }
      for (Map<String, Object> item : items) {
        PhotoAlbum album =
            new PhotoAlbum(
                (String) item.get("id"),
                (String) item.get("title"),
                (String) item.get("description"));
        albums.add(album);
      }
      page++;
    }
  }

  /**
   * Queries for the photos in albums.
   *
   * <p>This request doesn't support pages so it retrieves all photos at once for each album.
   *
   * @param authData authentication information
   * @param albums albums to get photos for
   * @param photoItems items to add retrieved photos to
   */
  private void requestAlbumPhotos(
      TokensAndUrlAuthData authData,
      Set<PhotoAlbum> albums,
      HashMap<String, Map<String, Object>> photoItems)
      throws IOException {
    // get photos for the each album
    for (PhotoAlbum album : albums) {
      String albumId = album.getId();
      String url = String.format(albumPhotosUrlTemplate, albumId);

      List<Map<String, Object>> items = requestData(authData, url);
      // iterate through received photos, add album id and save the photo
      for (Map<String, Object> item : items) {
        String photoId = (String) item.get("id");
        item.put("albumId", albumId);
        photoItems.put(photoId, item);
      }
    }
  }

  /**
   * Queries all photos for the account. Chooses photos which are not included to the collection of
   * photos from albums. Iterates through pages until all photos are retrieved.
   *
   * @param authData authentication information
   * @param nonAlbumPhotos items to add retrieved non-album photos to
   * @param albumPhotos collection with photos from albums
   */
  private void requestNonAlbumPhotos(
      TokensAndUrlAuthData authData,
      HashMap<String, Map<String, Object>> nonAlbumPhotos,
      HashMap<String, Map<String, Object>> albumPhotos)
      throws IOException {
    int page = 0;
    // continue processing until there are no photos returned for the next page
    while (true) {
      String url = String.format(allPhotosUrlTemplate, page);
      List<Map<String, Object>> items = requestData(authData, url);
      if (items == null || items.size() == 0) {
        return;
      }

      // iterate through received photos, process with photos not added to any album
      for (Map<String, Object> item : items) {
        String photoId = (String) item.get("id");
        if (!albumPhotos.containsKey(photoId)) {
          // add default album id and save the photo
          item.put("albumId", defaultAlbumId);
          nonAlbumPhotos.put(photoId, item);
        }
      }
      page++;
    }
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

  /**
   * Converts received photos to the list of {@link PhotoModel}
   *
   * @param photoItems items to convert
   * @return converted photos
   */
  private List<PhotoModel> convertPhotoItems(HashMap<String, Map<String, Object>> photoItems) {
    List<PhotoModel> photoModels = new ArrayList<>();
    for (Map.Entry<String, Map<String, Object>> photoItem : photoItems.entrySet()) {
      PhotoModel photoModel =
          new PhotoModel(
              (String) photoItem.getValue().get("name"),
              (String) photoItem.getValue().get("link"),
              (String) photoItem.getValue().get("description"),
              (String) photoItem.getValue().get("type"),
              (String) photoItem.getValue().get("id"),
              (String) photoItem.getValue().get("albumId"),
              false);
      photoModels.add(photoModel);
    }
    return photoModels;
  }
}
