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

package org.datatransferproject.transfer.mastodon.social;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.UrlEncodedContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.util.IOUtils;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import org.datatransferproject.transfer.mastodon.model.Account;
import org.datatransferproject.transfer.mastodon.model.Status;

/** Helper methods for interacting with the Mastodon API **/
public class MastodonHttpUtilities {
  private static final String ACCOUNT_VERIFICATION_URL = "/api/v1/accounts/verify_credentials";
  private static final String STATUS_URL_PATTERN = "/api/v1/accounts/%s/statuses";
  private static final String POST_URL = "/api/v1/statuses";

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final HttpTransport TRANSPORT = new NetHttpTransport();
  private final URI baseUri;
  private final String accessToken;
  private final String baseUrl;
  private final Account account;

  /**
   * Construct a new utility class for a given user.
   * @param accessToken the access token for a user from the UI or OAuth flow
   * @param baseUrl the base url of the mastodon instance of the user
   */
  MastodonHttpUtilities(String accessToken, String baseUrl) throws IOException {
    this.accessToken = checkNotNull(accessToken, "accessToken must be provided" );
    this.baseUrl = checkNotNull(baseUrl, "baseUrl must be provided");
    this.baseUri = URI.create(baseUrl);
    this.account = fetchAccount();
  }

  /** Gets the account info via verify_credentials. **/
  public Account getAccount() {
    return account;
  }

  /** Gets the statuses posted by the user. **/
  public Status[] getStatuses(String maxId) throws Exception {
    String url = String.format(STATUS_URL_PATTERN, account.getId());
    if (!Strings.isNullOrEmpty(maxId)) {
      url += "?max_id=" + maxId;
    }

    return request(url, Status[].class);
  }

  /** Posts a new status for the user, initially marked as private.**/
  public void postStatus(String content, String idempotencyKey) throws IOException {
    ImmutableMap<String, String> formParams = ImmutableMap.of(
        "status", content,
        // Default everything to private to avoid a privacy incident
        "visibility", "private"
    );
    UrlEncodedContent urlEncodedContent = new UrlEncodedContent(formParams);
    HttpRequest postRequest = TRANSPORT.createRequestFactory()
        .buildPostRequest(
            new GenericUrl(baseUrl + POST_URL),
            urlEncodedContent)
        .setThrowExceptionOnExecuteError(false);
    HttpHeaders headers = new HttpHeaders();
    headers.setAuthorization("Bearer " + accessToken);
    if (!Strings.isNullOrEmpty(idempotencyKey)) {
      // This prevents the same post from being posted twice in the case of network errors
      headers.set("Idempotency-Key", idempotencyKey);
    }
    postRequest.setHeaders(headers);

    HttpResponse response = postRequest.execute();

    validateResponse(postRequest, response, 200);
  }

  /** Gets the account info via verify_credentials. **/
  private Account fetchAccount() throws IOException {
    Account accountInfo = request(ACCOUNT_VERIFICATION_URL, Account.class);
    return accountInfo;
  }

  private <T> T request(String path, Class<T> clazz) throws IOException {
    String rawString = requestRaw(path);
    try {
      return OBJECT_MAPPER.readValue(rawString, clazz);
    } catch (IOException | RuntimeException e) {
      throw new IOException("Problem parsing results of: " + path + "\nContent: " + rawString, e);
    }
  }

  private String requestRaw(String path) throws IOException {
    HttpRequest getRequest = TRANSPORT.createRequestFactory().buildGetRequest(
        new GenericUrl(baseUrl + path));
    HttpHeaders headers = new HttpHeaders();
    headers.setAuthorization("Bearer " + accessToken);
    getRequest.setHeaders(headers);

    HttpResponse response = getRequest.execute();

    validateResponse(getRequest, response, 200);
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

    IOUtils.copy(response.getContent(), byteArrayOutputStream, true);
    return byteArrayOutputStream.toString();
  }

  private static void validateResponse(
      HttpRequest request, HttpResponse response, int expectedCode) throws IOException {
    if (response.getStatusCode() != expectedCode) {
      throw new IOException("Unexpected return code: "
          + response.getStatusCode()
          + "\nMessage:\n"
          + response.getStatusMessage()
          + "\nfrom:\n"
          + request.getUrl()
          + "\nHeaders:\n"
          + response.getHeaders());
    }
  }

  /** Gets the host name of the Mastodon instance for this user. **/
  public String getHostName() {
    return baseUri.getHost();
  }
}
