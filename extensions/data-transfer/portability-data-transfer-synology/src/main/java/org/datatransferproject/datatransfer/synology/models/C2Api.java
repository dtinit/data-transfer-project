/*
 * Copyright 2025 The Data Transfer Project Authors.
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
 *
 */

package org.datatransferproject.datatransfer.synology.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.datatransferproject.datatransfer.synology.utils.UrlUtils;

/** Data model for Synology C2 API configuration. */
public class C2Api extends ServiceConfig.Service {
  private final ApiPath apiPath;

  private final String createAlbum;
  private final String uploadItem;
  private final String addItemToAlbum;
  private final String signalJob;

  @JsonCreator
  public C2Api(@JsonProperty("baseUrl") String baseUrl, @JsonProperty("apiPath") ApiPath apiPath) {
    super(baseUrl);

    this.apiPath = apiPath;

    this.createAlbum = UrlUtils.join(baseUrl, apiPath.getCreateAlbumPath());
    this.uploadItem = UrlUtils.join(baseUrl, apiPath.getUploadItemPath());
    this.addItemToAlbum = UrlUtils.join(baseUrl, apiPath.getAddItemToAlbumPath());
    this.signalJob = UrlUtils.join(baseUrl, apiPath.getSignalJobPath());
  }

  public ApiPath getApiPath() {
    return apiPath;
  }

  public String getCreateAlbum() {
    return createAlbum;
  }

  public String getUploadItem() {
    return uploadItem;
  }

  public String getAddItemToAlbum() {
    return addItemToAlbum;
  }

  public String getSignalJob() {
    return signalJob;
  }

  public static class ApiPath {
    private final String createAlbumPath;
    private final String uploadItemPath;
    private final String addItemToAlbumPath;
    private final String signalJobPath;

    @JsonCreator
    public ApiPath(
        @JsonProperty("createAlbum") String createAlbumPath,
        @JsonProperty("uploadItem") String uploadItemPath,
        @JsonProperty("addItemToAlbum") String addItemToAlbumPath,
        @JsonProperty("signalJob") String signalJobPath) {
      this.createAlbumPath = createAlbumPath;
      this.uploadItemPath = uploadItemPath;
      this.addItemToAlbumPath = addItemToAlbumPath;
      this.signalJobPath = signalJobPath;
    }

    public String getCreateAlbumPath() {
      return createAlbumPath;
    }

    public String getUploadItemPath() {
      return uploadItemPath;
    }

    public String getAddItemToAlbumPath() {
      return addItemToAlbumPath;
    }

    public String getSignalJobPath() {
      return signalJobPath;
    }
  }
}
