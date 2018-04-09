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

package org.dataportabilityproject.auth.rememberthemilk;

import com.fasterxml.jackson.xml.XmlMapper;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import java.io.IOException;
import java.net.URL;
import org.dataportabilityproject.auth.rememberthemilk.model.AuthElement;
import org.dataportabilityproject.spi.api.auth.AuthDataGenerator;
import org.dataportabilityproject.spi.api.auth.AuthServiceProviderRegistry.AuthMode;
import org.dataportabilityproject.spi.api.types.AuthFlowConfiguration;
import org.dataportabilityproject.types.transfer.auth.AppCredentials;
import org.dataportabilityproject.types.transfer.auth.AuthData;
import org.dataportabilityproject.types.transfer.auth.TokenAuthData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RememberTheMilkAuthDataGenerator implements AuthDataGenerator {
  private static final String AUTH_URL = "http://api.rememberthemilk.com/services/auth/";
  private static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();
  private static final String GET_TOKEN_URL =
      "https://api.rememberthemilk.com/services/rest/?method=rtm.auth.getToken";

  private final Logger logger = LoggerFactory.getLogger(RememberTheMilkAuthDataGenerator.class);
  private final RememberTheMilkSignatureGenerator signatureGenerator;
  private final String perms;
  private final XmlMapper xmlMapper;

  public RememberTheMilkAuthDataGenerator(
      AppCredentials appCredentials, AuthMode authMode) {
    signatureGenerator = new RememberTheMilkSignatureGenerator(appCredentials);
    perms = (authMode == AuthMode.IMPORT) ? "write" : "read";
    this.xmlMapper = new XmlMapper();
  }

  @Override
  public AuthFlowConfiguration generateConfiguration(String callbackBaseUrl, String id) {
    // NOTE: callbackBaseUrl is unused. After authentication, RememberTheMilk will redirect
    // to the callback set when we configured the application. To change this visit:
    // https://www.rememberthemilk.com/help/contact/support/?ctx=api.update&report=1
    URL authUrlSigned;
    try {
      URL authUrlUnsigned = new URL(AUTH_URL + "?perms=" + perms);
      authUrlSigned = signatureGenerator.getSignature(authUrlUnsigned);
    } catch (Exception e) {
      logger.warn("Error generating Authentication URL: {}", e.getMessage());
      return null;
    }

    return new AuthFlowConfiguration(authUrlSigned.toString(), null);
  }

  @Override
  public AuthData generateAuthData(
      String callbackBaseUrl, String authCode, String id, AuthData initialAuthData, String extra) {
    // callbackbaseurl, id, initialAuthData and extra are not used.
    try {
      return new TokenAuthData(getToken(authCode));
    } catch (IOException e) {
      logger.warn("Error getting AuthToken: " + e.getMessage());
      return null;
    }
  }

  private String getToken(String frob) throws IOException {
    URL url = new URL(GET_TOKEN_URL + "&frob=" + frob);
    URL signedUrl = signatureGenerator.getSignature(url);

    HttpRequestFactory requestFactory = HTTP_TRANSPORT.createRequestFactory();
    HttpRequest getRequest = requestFactory.buildGetRequest(new GenericUrl(signedUrl));
    HttpResponse response = getRequest.execute();
    int statusCode = response.getStatusCode();
    if (statusCode != 200) {
      throw new IOException(
          "Bad status code: " + statusCode + " error: " + response.getStatusMessage());
    }

    AuthElement authElement = xmlMapper.readValue(response.getContent(), AuthElement.class);

    Preconditions.checkState(authElement.stat.equals("ok"), "state must be ok: %s", authElement);
    Preconditions.checkState(
        !Strings.isNullOrEmpty(authElement.auth.token), "token must not be empty", authElement);
    return authElement.auth.token;
  }
}
