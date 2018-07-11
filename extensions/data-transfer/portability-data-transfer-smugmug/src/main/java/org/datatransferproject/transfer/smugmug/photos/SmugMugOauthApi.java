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

package org.datatransferproject.transfer.smugmug.photos;

import org.scribe.builder.api.DefaultApi10a;
import org.scribe.model.Token;

/* Smugmug API to use with Scribe Service Builder
 * URLs provided from the Oauth1.0a flow described here:
 * https://api.smugmug.com/api/v2/doc/tutorial/authorization.html
 */
public class SmugMugOauthApi extends DefaultApi10a {

  @Override
  public String getRequestTokenEndpoint() {
    return "https://secure.smugmug.com/services/oauth/1.0a/getRequestToken";
  }

  @Override
  public String getAccessTokenEndpoint() {
    return "https://secure.smugmug.com/services/oauth/1.0a/getAccessToken";
  }

  @Override
  public String getAuthorizationUrl(Token requestToken) {
    return "https://secure.smugmug.com/services/oauth/1.0a/authorize?oauth_token="
        + requestToken.getToken();
  }
}
