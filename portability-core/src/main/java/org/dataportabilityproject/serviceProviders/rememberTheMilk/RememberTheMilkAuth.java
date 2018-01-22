/*
 * Copyright 2017 Google Inc.
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
package org.dataportabilityproject.serviceProviders.rememberTheMilk;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.google.api.client.xml.XmlNamespaceDictionary;
import com.google.api.client.xml.XmlObjectParser;
import java.io.IOException;
import java.net.URL;
import org.dataportabilityproject.serviceProviders.rememberTheMilk.model.AuthElement;
import org.dataportabilityproject.serviceProviders.rememberTheMilk.model.Frob;
import org.dataportabilityproject.shared.IOInterface;
import org.dataportabilityproject.shared.ServiceMode;
import org.dataportabilityproject.shared.auth.AuthData;
import org.dataportabilityproject.shared.auth.OfflineAuthDataGenerator;
import org.dataportabilityproject.shared.auth.SecretAuthData;

/**
 * Generates a token using the flow described: https://www.rememberthemilk.com/services/api/authentication.rtm
 */
public class RememberTheMilkAuth implements OfflineAuthDataGenerator {

  private static final String AUTH_URL = "http://api.rememberthemilk.com/services/auth/";
  private static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();
  private final RememberTheMilkSignatureGenerator signatureGenerator;
  private final ServiceMode serviceMode;
  private AuthElement authElement;

  RememberTheMilkAuth(RememberTheMilkSignatureGenerator signatureGenerator,
      ServiceMode serviceMode) {
    this.signatureGenerator = signatureGenerator;
    this.serviceMode = serviceMode;
  }

  @Override
  public AuthData generateAuthData(IOInterface ioInterface) throws IOException {
    return SecretAuthData.create(getToken(false, ioInterface));
  }

  String getToken(AuthData authData) {
    checkArgument(authData instanceof SecretAuthData,
        "authData expected to be SecretAuthData not %s",
        authData.getClass().getCanonicalName());
    return ((SecretAuthData) authData).secret();
  }

  private String getToken(boolean force, IOInterface ioInterface) throws IOException {
    if (authElement == null || force) {
      String frob = getFrob();
      presentLinkToUser(frob, ioInterface);
      authElement = getAuthToken(frob);
    }

    return authElement.auth.token;
  }

  private AuthElement validateToken(String auth_token) throws IOException {
    URL url = new URL(RememberTheMilkMethods.CHECK_TOKEN.getUrl());
    URL signedUrl = signatureGenerator.getSignature(url);

    HttpRequestFactory requestFactory = HTTP_TRANSPORT.createRequestFactory();
    HttpRequest getRequest = requestFactory.buildGetRequest(new GenericUrl(signedUrl));
    getRequest.setParser(new XmlObjectParser(new XmlNamespaceDictionary().set("", "")));
    HttpResponse response = getRequest.execute();
    int statusCode = response.getStatusCode();
    if (statusCode != 200) {
      throw new IOException(
          "Bad status code: " + statusCode + " error: " + response.getStatusMessage());
    }

    AuthElement authElement = response.parseAs(AuthElement.class);
    checkState(authElement.stat.equals("ok"), "state must be ok: %s", authElement);
    checkState(!Strings.isNullOrEmpty(authElement.auth.token), "token must not be empty",
        authElement);
    return authElement;
  }

  private String getFrob() throws IOException {
    URL url = new URL(RememberTheMilkMethods.GET_FROB.getUrl());
    URL signedUrl = signatureGenerator.getSignature(url);

    HttpRequestFactory requestFactory = HTTP_TRANSPORT.createRequestFactory();
    HttpRequest getRequest = requestFactory.buildGetRequest(new GenericUrl(signedUrl));
    getRequest.setParser(new XmlObjectParser(new XmlNamespaceDictionary().set("", "")));
    HttpResponse response = getRequest.execute();
    int statusCode = response.getStatusCode();
    if (statusCode != 200) {
      throw new IOException(
          "Bad status code: " + statusCode + " error: " + response.getStatusMessage());
    }
    Frob frob = response.parseAs(Frob.class);

    checkState(frob.stat.equals("ok"), "frob state must be ok: %s", frob);
    checkState(!Strings.isNullOrEmpty(frob.frob), "frob must not be empty", frob);
    return frob.frob;
  }

  private void presentLinkToUser(String frob, IOInterface ioInterface) throws IOException {
    String perms = (serviceMode == ServiceMode.EXPORT) ? "read" : "write";
    URL authUrlUnsigned = new URL(AUTH_URL + "?perms=" + perms + "&frob=" + frob);
    URL authUrlSigned = signatureGenerator.getSignature(authUrlUnsigned);

    ioInterface
        .ask("Please visit " + authUrlSigned + " and flow the flow there then hit return/enter");
  }

  private AuthElement getAuthToken(String frob) throws IOException {
    URL url = new URL(RememberTheMilkMethods.GET_TOKEN.getUrl() + "&frob=" + frob);
    URL signedUrl = signatureGenerator.getSignature(url);

    HttpRequestFactory requestFactory = HTTP_TRANSPORT.createRequestFactory();
    HttpRequest getRequest = requestFactory.buildGetRequest(new GenericUrl(signedUrl));
    getRequest.setParser(new XmlObjectParser(new XmlNamespaceDictionary().set("", "")));
    HttpResponse response = getRequest.execute();
    int statusCode = response.getStatusCode();
    if (statusCode != 200) {
      throw new IOException(
          "Bad status code: " + statusCode + " error: " + response.getStatusMessage());
    }
    AuthElement authElement = response.parseAs(AuthElement.class);
    checkState(authElement.stat.equals("ok"), "state must be ok: %s", authElement);
    checkState(!Strings.isNullOrEmpty(authElement.auth.token), "token must not be empty",
        authElement);
    System.out.println("Auth Token: " + authElement);
    return authElement;
  }
}
