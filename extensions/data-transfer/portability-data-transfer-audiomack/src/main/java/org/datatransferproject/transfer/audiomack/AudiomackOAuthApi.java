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

package org.datatransferproject.transfer.audiomack;

import org.scribe.builder.api.DefaultApi10a;
import org.scribe.model.Token;

public class AudiomackOAuthApi extends DefaultApi10a {

  @Override
  public String getRequestTokenEndpoint() {
    return "https://api.audiomack.com/v1/request_token";
  }

  @Override
  public String getAccessTokenEndpoint() {
    return "https://api.audiomack.com/v1/access_token";
  }

  @Override
  public String getAuthorizationUrl(Token requestToken) {
    return "https://www.autdiomack.com/oauth/authenticate?oauth_token=" + requestToken.getToken();
  }
}
