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

/*
 * Copyright 2018 The Data Transfer Project Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.datatransferproject.datatransfer.google.videos;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.json.JsonFactory;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import org.datatransferproject.datatransfer.google.common.GoogleCredentialFactory;
import org.datatransferproject.datatransfer.google.mediaModels.NewMediaItem;
import org.datatransferproject.datatransfer.google.mediaModels.NewMediaItemUpload;
import org.datatransferproject.spi.cloud.storage.JobStore;
import org.datatransferproject.spi.transfer.provider.ImportResult;
import org.datatransferproject.spi.transfer.provider.Importer;
import org.datatransferproject.transfer.ImageStreamProvider;
import org.datatransferproject.types.common.models.videos.VideoObject;
import org.datatransferproject.types.common.models.videos.VideosContainerResource;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;
import org.datatransferproject.api.launcher.Monitor;


import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.UUID;

public class GoogleVideosImporter
        implements Importer<TokensAndUrlAuthData, VideosContainerResource> {

  // TODO: internationalize copy prefix
  private static final String COPY_PREFIX = "Copy of ";

  private final GoogleCredentialFactory credentialFactory;
  private final JobStore jobStore;
  private final ImageStreamProvider videoStreamProvider;
  private volatile GoogleVideosInterface videosInterface;
  private Monitor monitor;
  private JsonFactory jsonFactory;

  public GoogleVideosImporter(GoogleCredentialFactory credentialFactory, JobStore jobStore, JsonFactory jsonFactory, Monitor monitor) {
    this(credentialFactory, jobStore, null, new ImageStreamProvider(), jsonFactory, monitor);
  }

  @VisibleForTesting
  GoogleVideosImporter(
          GoogleCredentialFactory credentialFactory,
          JobStore jobStore,
          GoogleVideosInterface videosInterface,
          ImageStreamProvider videoStreamProvider,
          JsonFactory jsonFactory,
          Monitor monitor) {
    this.credentialFactory = credentialFactory;
    this.jobStore = jobStore;
    this.videosInterface = videosInterface;
    this.videoStreamProvider = videoStreamProvider;
    this.jsonFactory = jsonFactory;
    this.monitor = monitor;
  }

  @Override
  public ImportResult importItem(
          UUID jobId, TokensAndUrlAuthData authData, VideosContainerResource data) throws IOException {
    if (data == null) {
      // Nothing to do
      return ImportResult.OK;
    }

    //     Uploads videos
    if (data.getVideos() != null && data.getVideos().size() > 0) {
      for (VideoObject video : data.getVideos()) {
        importSingleVideo(jobId, authData, video);
      }
    }
    return ImportResult.OK;
  }

  void importSingleVideo(UUID jobId, TokensAndUrlAuthData authData, VideoObject inputVideo)
          throws IOException {

    // download video and create input stream
    InputStream inputStream;
    if (inputVideo.getContentUrl() == null) {
      monitor.info(
              () ->
                      "Content Url is empty. Make sure that you provide a valid content Url.");
      return;
    }
    
    inputStream = this.videoStreamProvider.get(inputVideo.getContentUrl().toString());

    String filename;
    if (Strings.isNullOrEmpty(inputVideo.getName())) {
      filename = "untitled";
    } else {
      filename = COPY_PREFIX + inputVideo.getName();
    }

    String uploadToken =
            getOrCreateVideosInterface(authData).uploadVideoContent(inputStream, filename);

    NewMediaItem newMediaItem = new NewMediaItem(filename, uploadToken);

    NewMediaItemUpload uploadItem =
            new NewMediaItemUpload(null, Collections.singletonList(newMediaItem));

    getOrCreateVideosInterface(authData).createVideo(uploadItem);
  }

  private synchronized GoogleVideosInterface getOrCreateVideosInterface(
          TokensAndUrlAuthData authData) {
    return videosInterface == null ? makeVideosInterface(authData) : videosInterface;
  }

  private synchronized GoogleVideosInterface makeVideosInterface(TokensAndUrlAuthData authData) {
    Credential credential = credentialFactory.createCredential(authData);
    GoogleVideosInterface videosInterface = new GoogleVideosInterface(credential, this.jsonFactory);
    return videosInterface;
  }
}
