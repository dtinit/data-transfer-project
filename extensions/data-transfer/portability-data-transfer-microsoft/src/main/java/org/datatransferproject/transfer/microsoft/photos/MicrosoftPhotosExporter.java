/*
 * Copyright 2018 The Data-Portability Project Authors.
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
package org.datatransferproject.transfer.microsoft.photos;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.datatransferproject.spi.cloud.storage.JobStore;
import org.datatransferproject.spi.transfer.provider.ExportResult;
import org.datatransferproject.spi.transfer.provider.Exporter;
import org.datatransferproject.types.common.ExportInformation;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;
import org.datatransferproject.types.common.models.photos.PhotoAlbum;
import org.datatransferproject.types.common.models.photos.PhotoModel;
import org.datatransferproject.types.common.models.photos.PhotosContainerResource;

/**
 * Exports Microsoft OneDrive photos using the Graph API.
 *
 * <p>Converts folders to albums.
 */
public class MicrosoftPhotosExporter implements
    Exporter<TokensAndUrlAuthData, PhotosContainerResource> {

  private static final String ODATA_NEXT = "@odata.nextLink";
  private final String photosRootUrl;
  private final String photosFolderTemplate;
  private final String photosContentTemplate;

  private final OkHttpClient client;
  private final ObjectMapper objectMapper;
  private final JobStore jobStore;

  public MicrosoftPhotosExporter(
      String baseUrl, OkHttpClient client, ObjectMapper objectMapper, JobStore jobStore) {
    photosRootUrl = baseUrl + "/v1.0/me/drive/special/photos/children";
    photosFolderTemplate = baseUrl + "/v1.0/me/drive/items/%s/children";
    photosContentTemplate = baseUrl + "/v1.0/me/drive/items/%s/content";
    this.client = client;
    this.objectMapper = objectMapper;
    this.jobStore = jobStore;
  }

  @Override
  public ExportResult<PhotosContainerResource> export(UUID jobId, TokensAndUrlAuthData authData,
      Optional<ExportInformation> exportInformation) {

    try {

      List<Map<String, Object>> photoItems = requestAllPhotoItems(authData);

      downloadAndCachePhotos(jobId, authData, photoItems);

      PhotosContainerResource containerResource = convertToResource(photoItems);

      return new ExportResult<>(ExportResult.ResultType.END, containerResource);

    } catch (IOException e) {
      e.printStackTrace(); // FIXME log error
      return new ExportResult<>(e);
    }
  }

  /**
   * Recursively scans OneDrive folders starting with the special root photo folder. Returns all
   * photo items, i.e. items with an image MIME type.
   *
   * <p>Folders are recursively processed using a deque of folder items. The deque is iterated, a
   * dequeue folder item is popped, its contained items are requested, and any contained folders are
   * added to the end of the dequeue. Processing completes when the dequeue has been exhausted.
   *
   * <p>All contained photo items are returned.
   *
   * @param authData the authorization data
   */
  private List<Map<String, Object>> requestAllPhotoItems(TokensAndUrlAuthData authData)
      throws IOException {
    List<Map<String, Object>> photoItems = new ArrayList<>();

    Deque<Map<String, Object>> folderItems = new ArrayDeque<>();

    requestItems(photosRootUrl, authData, folderItems, photoItems);

    // request items for all folders recursively until the collection is exhausted, adding contained
    // folders to the end of the deque for processing
    while (!folderItems.isEmpty()) {
      Map<String, Object> folderItem = folderItems.removeLast();
      String id = (String) folderItem.get("id");
      String url = String.format(photosFolderTemplate, id);

      requestItems(url, authData, folderItems, photoItems);
    }

    return photoItems;
  }

  /**
   * Scans a OneDrive folder for items, adding folders and photos to the provided collections.
   *
   * @param url the folder URL
   * @param authData auth data
   * @param folderItems the folder collection to add contained folders to
   * @param photoItems the photo collection to add contained photos to
   */
  @SuppressWarnings("unchecked")
  private void requestItems(
      String url,
      TokensAndUrlAuthData authData,
      Deque<Map<String, Object>> folderItems,
      List<Map<String, Object>> photoItems)
      throws IOException {
    // continue processing paginated results until there is no next url to request from
    while (url != null) {
      Request.Builder requestBuilder = new Request.Builder().url(url);
      requestBuilder.header("Authorization", "Bearer " + authData.getAccessToken());

      try (Response graphResponse = client.newCall(requestBuilder.build()).execute()) {
        ResponseBody body = graphResponse.body();
        if (body == null) {
          return;
        }

        String contentBody = new String(body.bytes());

        Map contentMap = objectMapper.reader().forType(Map.class).readValue(contentBody);

        url = (String) contentMap.get(ODATA_NEXT);

        List<Map<String, Object>> items = (List<Map<String, Object>>) contentMap.get("value");
        if (items == null) {
          return;
        }
        for (Map<String, Object> item : items) {
          if (item.containsKey("folder")) {
            folderItems.add(item);
          } else {
            Map<String, Object> fileData = (Map<String, Object>) item.get("file");
            if (fileData != null) {
              String mimeType = (String) fileData.get("mimeType");
              if (mimeType != null && mimeType.startsWith("image/")) {
                photoItems.add(item);
              }
            }
          }
        }
      }
    }
  }

  /**
   * Downloads the photo items to the job store.
   */
  private void downloadAndCachePhotos(
      UUID jobId, TokensAndUrlAuthData authData, List<Map<String, Object>> photoItems) {
    for (Map<String, Object> photoItem : photoItems) {

      String id = (String) photoItem.get("id");
      String url = String.format(photosContentTemplate, id);

      Request.Builder requestBuilder = new Request.Builder().url(url);
      requestBuilder.header("Authorization", "Bearer " + authData.getAccessToken());

      try (Response graphResponse = client.newCall(requestBuilder.build()).execute()) {
        ResponseBody body = graphResponse.body();
        if (body == null) {
          continue;
        }
        // TODO Encrypt this!
        jobStore.create(jobId, id, body.byteStream());

      } catch (IOException e) {
        // skip the photo
        e.printStackTrace(); // FIXME log error
      }
    }
  }

  @SuppressWarnings("unchecked")
  private PhotosContainerResource convertToResource(List<Map<String, Object>> photoItems) {

    Set<PhotoAlbum> albums = new HashSet<>();
    List<PhotoModel> photoModels = new ArrayList<>();

    for (Map<String, Object> photoItem : photoItems) {
      String id = (String) photoItem.get("id");
      String name = (String) photoItem.get("name");

      // Note file and MIME type data are guaranteed to be present
      Map<String, Object> fileData = (Map<String, Object>) photoItem.get("file");
      String mimeType = (String) fileData.get("mimeType");

      // NB: descriptions are not available in OneDrive

      String albumId = null;
      // convert the parent folder to an album
      Map<String, Object> parentReference = (Map<String, Object>) photoItem.get("parentReference");
      if (parentReference != null) {
        albumId = (String) parentReference.get("id");
        String parentName = (String) parentReference.get("name");
        PhotoAlbum album = new PhotoAlbum(albumId, parentName, "");
        albums.add(album);
      }
      PhotoModel photoModel = new PhotoModel(name, null, "", mimeType, id, albumId, false);

      photoModels.add(photoModel);
    }

    return new PhotosContainerResource(albums, photoModels);
  }
}
