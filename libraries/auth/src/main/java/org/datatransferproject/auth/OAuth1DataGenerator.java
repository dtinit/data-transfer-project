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

package org.datatransferproject.auth;

import com.google.api.client.http.HttpTransport;
import java.util.Set;
import org.datatransferproject.spi.api.auth.AuthDataGenerator;
import org.datatransferproject.spi.api.auth.AuthServiceProviderRegistry.AuthMode;
import org.datatransferproject.spi.api.types.AuthFlowConfiguration;
import org.datatransferproject.types.transfer.auth.AppCredentials;
import org.datatransferproject.types.transfer.auth.AuthData;

public class OAuth1DataGenerator implements AuthDataGenerator {

  private final OAuth1Config config;
  private final Set<String> scopes;
  // TODO: handle dynamic updates of client ids and secrets #597
  private final String clientId;
  private final String clientSecret;
  private final HttpTransport httpTransport;

  OAuth1DataGenerator(OAuth1Config config, AppCredentials appCredentials,
      HttpTransport httpTransport, String datatype, AuthMode mode) {
    this.config = config;
    this.clientId = appCredentials.getKey();
    this.clientSecret = appCredentials.getSecret();
    this.httpTransport = httpTransport;
    this.scopes = mode == AuthMode.EXPORT
        ? config.getExportScopes().get(datatype)
        : config.getImportScopes().get(datatype);
  }

  @Override
  public AuthFlowConfiguration generateConfiguration(String callbackBaseUrl, String id) {
    return null;
  }

  @Override
  public AuthData generateAuthData(String callbackBaseUrl, String authCode, String id,
      AuthData initialAuthData, String extra) {
    return null;
  }
}
