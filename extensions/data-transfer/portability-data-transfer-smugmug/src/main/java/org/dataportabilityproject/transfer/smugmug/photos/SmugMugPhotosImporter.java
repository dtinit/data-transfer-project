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

package org.dataportabilityproject.transfer.smugmug.photos;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.api.client.http.HttpContent;
import com.google.api.client.http.InputStreamContent;
import com.google.api.client.http.json.JsonHttpContent;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.common.annotations.VisibleForTesting;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import org.dataportabilityproject.spi.cloud.storage.JobStore;
import org.dataportabilityproject.spi.transfer.provider.ImportResult;
import org.dataportabilityproject.spi.transfer.provider.Importer;
import org.dataportabilityproject.spi.transfer.provider.ImportResult.ResultType;
import org.dataportabilityproject.spi.transfer.types.TempPhotosData;
import org.dataportabilityproject.transfer.smugmug.photos.model.ImageUploadResponse;
import org.dataportabilityproject.transfer.smugmug.photos.model.SmugMugAlbumResponse;
import org.dataportabilityproject.transfer.smugmug.photos.model.SmugMugResponse;
import org.dataportabilityproject.transfer.smugmug.photos.model.SmugMugUserResponse;
import org.dataportabilityproject.types.transfer.auth.AuthData;
import org.dataportabilityproject.types.transfer.models.photos.PhotoAlbum;
import org.dataportabilityproject.types.transfer.models.photos.PhotoModel;
import org.dataportabilityproject.types.transfer.models.photos.PhotosContainerResource;

import static com.google.common.base.Preconditions.checkState;

public class SmugMugPhotosImporter implements Importer<AuthData, PhotosContainerResource> {

  private SmugMugInterface smugMugInterface;
  private final JobStore jobStore;

  @VisibleForTesting
  SmugMugPhotosImporter(SmugMugInterface smugMugInterface, JobStore jobStore) {
    this.smugMugInterface = smugMugInterface;
    this.jobStore = jobStore;
  }

  @Override
  public ImportResult importItem(UUID jobId, AuthData authData, PhotosContainerResource data) {
    try {
      String folder = null;
      if (!data.getAlbums().isEmpty()) {
        SmugMugResponse<SmugMugUserResponse> userResponse = smugMugInterface.makeUserRequest
                (smugMugInterface.USER_URL);
        folder = userResponse.getResponse().getUser().getUris().get("Folder").getUri();
      }
      for (PhotoAlbum album : data.getAlbums()) {
        importSingleAlbum(jobId, folder, album);
      }
      for (PhotoModel photo : data.getPhotos()) {
        importSinglePhoto(jobId, photo);
      }
    } catch (IOException e) {
      // TODO(olsona): we should retry on individual errors
      return new ImportResult(ResultType.ERROR, e.getMessage());
    }
    return ImportResult.OK;
  }

  @VisibleForTesting
  void importSingleAlbum(UUID jobId, String folder, PhotoAlbum inputAlbum) throws  IOException {
    // Set up album
    Map<String, String> json = new HashMap<>();
    String niceName = "Copy-" + inputAlbum.getName().replace(' ', '-');
    json.put("UrlName", niceName);
    // Allow conflicting names to be changed
    json.put("AutoRename", "true");
    json.put("Name", "Copy of " + inputAlbum.getName());
    // All imported content is private by default.
    json.put("Privacy", "Private");
    HttpContent content = new JsonHttpContent(new JacksonFactory(), json);

    // Upload album
    SmugMugResponse<SmugMugAlbumResponse> response =
            smugMugInterface.postRequest(
                    folder + "!albums",
                    content,
                    ImmutableMap.of(),
                    new TypeReference<SmugMugResponse<SmugMugAlbumResponse>>() {});
    checkState(response.getResponse() != null, "Response is null");
    checkState(response.getResponse().getAlbum() != null, "Album is null");

    // Put new album ID in job store so photos can be assigned to correct album
    // TODO(olsona): thread safety!
    TempPhotosData tempPhotosData = jobStore.findData(TempPhotosData.class, jobId);
    if (tempPhotosData == null) {
      tempPhotosData = new TempPhotosData(jobId);
      jobStore.create(jobId, tempPhotosData);
    }
    tempPhotosData.addAlbumId(inputAlbum.getId(), response.getResponse().getAlbum().getAlbumKey());
  }

  @VisibleForTesting
  void importSinglePhoto(UUID jobId, PhotoModel inputPhoto) throws IOException {
    // Set up photo
    InputStreamContent content =
            new InputStreamContent(null, getImageAsStream(inputPhoto.getFetchableUrl()));

    // Find album to upload photo to
    String newAlbumKey = jobStore.findData(TempPhotosData.class, jobId).lookupNewAlbumId
            (inputPhoto.getAlbumId());
    checkState(
            !Strings.isNullOrEmpty(newAlbumKey), "Cached album key for %s is null", inputPhoto
                    .getAlbumId());

    // Upload photo
    smugMugInterface.postRequest(
            "http://upload.smugmug.com/",
            content,
            // Headers from: https://api.smugmug.com/api/v2/doc/reference/upload.html
            ImmutableMap.of(
                    "X-Smug-AlbumUri", "/api/v2/album/" + newAlbumKey,
                    "X-Smug-ResponseType", "json",
                    "X-Smug-Version", "v2"),
            new TypeReference<ImageUploadResponse>() {});

  }

  // Should pull this out into separate library
  private static InputStream getImageAsStream(String urlStr) throws IOException {
    URL url = new URL(urlStr);
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    conn.connect();
    return conn.getInputStream();
  }
}
