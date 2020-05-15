/*
 * Copyright 2020 The Data Transfer Project Authors.
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

package org.datatransferproject.transfer.facebook.videos;

import com.restfb.DefaultFacebookClient;
import com.restfb.FacebookClient;
import com.restfb.Parameter;
import com.restfb.Version;
import com.restfb.types.GraphResponse;
import java.util.ArrayList;
import java.util.UUID;
import org.datatransferproject.spi.transfer.idempotentexecutor.IdempotentImportExecutor;
import org.datatransferproject.spi.transfer.provider.ImportResult;
import org.datatransferproject.spi.transfer.provider.Importer;
import org.datatransferproject.types.common.models.videos.VideoObject;
import org.datatransferproject.types.common.models.videos.VideosContainerResource;
import org.datatransferproject.types.transfer.auth.AppCredentials;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;

public class FacebookVideosImporter
    implements Importer<TokensAndUrlAuthData, VideosContainerResource> {

  public FacebookVideosImporter(AppCredentials appCredentials) {
    this.appCredentials = appCredentials;
  }

  private AppCredentials appCredentials;

  @Override
  public ImportResult importItem(
      UUID jobId,
      IdempotentImportExecutor executor,
      TokensAndUrlAuthData authData,
      VideosContainerResource data) {

    DefaultFacebookClient client =
        new DefaultFacebookClient(
            authData.getAccessToken(), appCredentials.getSecret(), Version.VERSION_3_0);

    for (VideoObject video : data.getVideos()) {
      importSingleVideo(client, video);
    }

    return ImportResult.OK;
  }

  void importSingleVideo(FacebookClient client, VideoObject video) {
    ArrayList<Parameter> params = new ArrayList<>();
    params.add(Parameter.with("file_url", video.getContentUrl().toString()));
    if (video.getDescription() != null)
      params.add(Parameter.with("description", video.getDescription()));

    String endpoint = "me/videos";
    client.publish(endpoint, GraphResponse.class, params.toArray(new Parameter[0]));
  }
}
