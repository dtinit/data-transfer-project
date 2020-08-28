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

import com.restfb.Connection;
import com.restfb.DefaultFacebookClient;
import com.restfb.Parameter;
import com.restfb.Version;
import com.restfb.exception.FacebookOAuthException;
import com.restfb.types.Video;
import java.util.ArrayList;
import java.util.Optional;
import org.datatransferproject.spi.transfer.types.CopyExceptionWithFailureReason;
import org.datatransferproject.transfer.facebook.utils.FacebookTransferUtils;
import org.datatransferproject.types.transfer.auth.AppCredentials;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;

public class RestFbFacebookVideos implements FacebookVideosInterface {
  private DefaultFacebookClient client;

  RestFbFacebookVideos(TokensAndUrlAuthData authData, AppCredentials appCredentials) {
    client =
        new DefaultFacebookClient(
            authData.getAccessToken(), appCredentials.getSecret(), Version.VERSION_3_0);
  }

  public Connection<Video> getVideos(Optional<String> paginationToken)
      throws CopyExceptionWithFailureReason {
    ArrayList<Parameter> parameters = new ArrayList<>();
    parameters.add(Parameter.with("fields", "description,source"));
    paginationToken.ifPresent(token -> parameters.add(Parameter.with("after", token)));
    try {
      return client.fetchConnection(
          "me/videos/uploaded", Video.class, parameters.toArray(new Parameter[0]));
    } catch (FacebookOAuthException e) {
      throw FacebookTransferUtils.handleFacebookOAuthException(e);
    }
  }
}
