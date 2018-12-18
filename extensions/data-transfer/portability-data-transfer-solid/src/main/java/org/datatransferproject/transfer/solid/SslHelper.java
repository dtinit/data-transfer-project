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

package org.datatransferproject.transfer.solid;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.common.base.Strings;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.SecureRandom;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;


public class SslHelper {
  private static final String INRPUT_LOGIN_SERVER = "https://inrupt.net/login/tls";
  private final String pathToPkcs12File;
  private final String password;

  public SslHelper(String pathToPkcs12File, String password) {
    this.pathToPkcs12File = pathToPkcs12File;
    this.password = password;
  }

  /** Logs in in via WebTls and return the auth cookie to use**/
  public String loginViaCertificate() throws GeneralSecurityException, IOException  {
    SSLSocketFactory sslSocketFactory = getSocketFactory();

    HttpTransport transport = new NetHttpTransport.Builder()
        .setSslSocketFactory(sslSocketFactory)
        .build();
    return makeCall(transport);
  }

  private SSLSocketFactory getSocketFactory() throws GeneralSecurityException, IOException {
    KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
    KeyStore keyStore = KeyStore.getInstance("PKCS12");
    InputStream keyInput = new FileInputStream(pathToPkcs12File);
    keyStore.load(keyInput, password.toCharArray());
    keyInput.close();

    keyManagerFactory.init(keyStore, password.toCharArray());

    SSLContext context = SSLContext.getInstance("TLS");
    context.init(keyManagerFactory.getKeyManagers(), null, new SecureRandom());

    return context.getSocketFactory();
  }

  private String makeCall(HttpTransport transport) throws IOException {
    HttpRequest get =
        transport.createRequestFactory()
            .buildPostRequest(new GenericUrl(INRPUT_LOGIN_SERVER), null)
            .setFollowRedirects(false)
            .setThrowExceptionOnExecuteError(false);

    HttpResponse response = get.execute();
    if (response.getStatusCode() != 302) {
      throw new IOException("Unexpected return code: "
          + response.getStatusCode()
          + "\nMessage:\n"
          + response.getStatusMessage());
    }
    String cookieValue = response.getHeaders().getFirstHeaderStringValue("set-cookie");
    if (Strings.isNullOrEmpty(cookieValue)) {
      throw new IOException("Couldn't extract cookie value from headers: " + response.getHeaders());
    }
    return cookieValue;
  }
}
