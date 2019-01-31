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

public class ImgurPhotosExporter
    implements Exporter<TokensAndUrlAuthData, PhotosContainerResource> {
  private final String photosUrlTemplate;
  private final String allPhotosUrlTemplate;
  private final String albumsUrlTemplate;
  private final String defaultAlbumId;

  private final OkHttpClient client;
  private final ObjectMapper objectMapper;

  private Monitor monitor;

  public ImgurPhotosExporter(Monitor monitor, String baseUrl) {
    photosUrlTemplate = baseUrl + "/album/%s/images/%s";
    albumsUrlTemplate = baseUrl + "/account/me/albums/%s";
    allPhotosUrlTemplate = baseUrl + "/account/me/images/%s";
    defaultAlbumId = "0";
    this.client = new OkHttpClient.Builder().build();
    this.objectMapper = new ObjectMapper();
    this.monitor = monitor;
  }

  @Override
  public ExportResult<PhotosContainerResource> export(
      UUID jobId, TokensAndUrlAuthData authData, Optional<ExportInformation> exportInformation)
      throws Exception {
    HashMap photoItems = new HashMap<String, Object>();
    HashMap nonAlbumPhotoItems = new HashMap<String, Object>();

    Set<PhotoAlbum> albums = getAlbums(albumsUrlTemplate, authData);

    requestAlbumPhotos(authData, albums, photoItems);
    requestNonAlbumPhotos(authData, nonAlbumPhotoItems, photoItems);

    monitor.info(() -> "got nonAlbumPhotoItems.size() : %s ", nonAlbumPhotoItems.size());

    if (nonAlbumPhotoItems.size() > 0) {
      albums.add(new PhotoAlbum(defaultAlbumId, "Non-album photos", "Contains Non-album photos"));
      photoItems.putAll(nonAlbumPhotoItems);
    }

    List<PhotoModel> photoModels = convertToResource(photoItems);
    monitor.info(
        () -> "-- albums.size(): %s photoModels.size(): %s", albums.size(), photoModels.size());

    return new ExportResult<>(
        ExportResult.ResultType.END, new PhotosContainerResource(albums, photoModels));
  }

  private Set<PhotoAlbum> getAlbums(String urlTemplate, TokensAndUrlAuthData authData)
      throws IOException {
    Set<PhotoAlbum> albums = new HashSet<>();
    int page = 0;
    while (true) {
      String url = String.format(urlTemplate, page);
      Request.Builder requestBuilder = new Request.Builder().url(url);
      monitor.info(() -> "getAlbums() url: %s", url);
      requestBuilder.header("Authorization", "Bearer " + authData.getAccessToken());
      try (Response response = client.newCall(requestBuilder.build()).execute()) {
        ResponseBody body = response.body();
        if (body == null) {
          return albums;
        }
        String contentBody = new String(body.bytes());
        Map contentMap = objectMapper.reader().forType(Map.class).readValue(contentBody);
        List<Map<String, Object>> items = (List<Map<String, Object>>) contentMap.get("data");
        if (items == null || items.size() == 0) {
          return albums;
        }
        for (Map<String, Object> item : items) {
          monitor.info(() -> "---- ALBUM id: %s title: %s", item.get("id"), item.get("title"));
          PhotoAlbum album =
              new PhotoAlbum(
                  (String) item.get("id"),
                  (String) item.get("title"),
                  (String) item.get("description"));
          albums.add(album);
        }
      }
      page++;
    }
  }

  private void requestAlbumPhotos(
      TokensAndUrlAuthData authData,
      Set<PhotoAlbum> albums,
      HashMap<String, Map<String, Object>> photoItems)
      throws IOException {
    monitor.info(() -> "-- requestAlbumPhotos(), folderItems.size(): %s", albums.size());
    for (PhotoAlbum album : albums) {
      int page = 0;
      String albumId = album.getId();
      String url = String.format(photosUrlTemplate, albumId, page);
      monitor.info(
          () -> "-- requestAlbumPhotos(), album id: %s title: %s, urlIm: %s",
          albumId,
          album.getName(),
          url);
      Request.Builder requestBuilder = new Request.Builder().url(url);
      requestBuilder.header("Authorization", "Bearer " + authData.getAccessToken());
      try (Response response = client.newCall(requestBuilder.build()).execute()) {
        ResponseBody body = response.body();
        if (body == null) {
          break;
        }
        String contentBody = new String(body.bytes());
        Map contentMap = objectMapper.reader().forType(Map.class).readValue(contentBody);
        List<Map<String, Object>> items = (List<Map<String, Object>>) contentMap.get("data");
        if (items == null || items.size() == 0) {
          break;
        }
        monitor.info(() -> "-- got Photos, items.size(): %s ", items.size());
        for (Map<String, Object> item : items) {
          String photoId = (String) item.get("id");
          item.put("albumId", albumId);
          photoItems.put(photoId, item);
          monitor.info(() -> "-- add Photo, items.size()");
        }
      }
    }
  }

  private void requestNonAlbumPhotos(
      TokensAndUrlAuthData authData,
      HashMap<String, Map<String, Object>> nonAlbumPhotos,
      HashMap<String, Map<String, Object>> albumPhotos)
      throws IOException {
    monitor.info(() -> "-- requestNonAlbumPhotos()");
    int page = 0;
    while (true) {
      String url = String.format(allPhotosUrlTemplate, page);
      monitor.info(() -> "-- requestNonAlbumPhotos(), url %s", url);
      monitor.info(() -> "-- albumPhotos.size(): %s", albumPhotos.size());

      Request.Builder requestBuilder = new Request.Builder().url(url);
      requestBuilder.header("Authorization", "Bearer " + authData.getAccessToken());

      try (Response response = client.newCall(requestBuilder.build()).execute()) {
        ResponseBody body = response.body();
        if (body == null) {
          return;
        }
        String contentBody = new String(body.bytes());
        Map contentMap = objectMapper.reader().forType(Map.class).readValue(contentBody);
        List<Map<String, Object>> items = (List<Map<String, Object>>) contentMap.get("data");
        monitor.info(() -> "-- requestNonAlbumPhotos(), items.size(): %s", items.size());
        if (items == null || items.size() == 0) {
          return;
        }

        for (Map<String, Object> item : items) {
          String photoId = (String) item.get("id");
          if (!albumPhotos.containsKey(photoId)) {
            item.put("albumId", defaultAlbumId);
            nonAlbumPhotos.put(photoId, item);
            monitor.info(() -> "-- add to nonAlbumPhotos, Photo: %s", photoId);
          } else {
            monitor.info(() -> "didn't add to nonAlbumPhotos, Photo: %s", photoId);
          }
        }
      }
      page++;
    }
  }

  private List<PhotoModel> convertToResource(HashMap<String, Map<String, Object>> photoItems) {
    monitor.info(() -> "convertToResource, size: %s", photoItems.size());
    List<PhotoModel> photoModels = new ArrayList<>();
    for (Map.Entry<String, Map<String, Object>> photoItem : photoItems.entrySet()) {
      String id = (String) photoItem.getValue().get("id");
      String name = (String) photoItem.getValue().get("name");
      String mimeType = (String) photoItem.getValue().get("type");
      String url = (String) photoItem.getValue().get("link");
      String description = (String) photoItem.getValue().get("description");
      String albumId = (String) photoItem.getValue().get("albumId");
      PhotoModel photoModel = new PhotoModel(name, url, description, mimeType, id, albumId, false);
      photoModels.add(photoModel);
      monitor.info(() -> "add photo, name: %s url: %s albumId: %s", name, url, albumId);
    }
    return photoModels;
  }
}
