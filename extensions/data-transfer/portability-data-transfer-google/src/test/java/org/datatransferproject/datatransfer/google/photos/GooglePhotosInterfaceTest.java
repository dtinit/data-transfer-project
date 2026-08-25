/*
 * Copyright 2026 The Data Transfer Project Authors.
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

package org.datatransferproject.datatransfer.google.photos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpContent;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.common.io.BaseEncoding;
import java.io.ByteArrayInputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.datatransfer.google.common.GoogleCredentialFactory;
import org.datatransferproject.spi.transfer.types.UploadErrorException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

public class GooglePhotosInterfaceTest {

  private static final String ACCESS_TOKEN = "test-access-token";
  private static final String SHA1 = "11aa11AAff11aa11AAFF11aa11AAff11aa11AAFF";

  private GooglePhotosInterface photosInterface;
  private HttpRequest httpRequest;
  private HttpRequestFactory requestFactory;
  private HttpHeaders headers;

  @BeforeEach
  public void setUp() throws Exception {
    GoogleCredentialFactory credentialFactory = mock(GoogleCredentialFactory.class);
    Credential credential = mock(Credential.class);
    when(credential.getAccessToken()).thenReturn(ACCESS_TOKEN);

    photosInterface =
        new GooglePhotosInterface(
            credentialFactory, credential, GsonFactory.getDefaultInstance(), mock(Monitor.class), 1.0);

    HttpTransport httpTransport = mock(HttpTransport.class);
    requestFactory = mock(HttpRequestFactory.class);
    httpRequest = mock(HttpRequest.class);
    headers = new HttpHeaders();
    when(httpRequest.getHeaders()).thenReturn(headers);
    when(httpTransport.createRequestFactory()).thenReturn(requestFactory);
    when(requestFactory.buildPostRequest(any(GenericUrl.class), any(HttpContent.class)))
        .thenReturn(httpRequest);

    setField(photosInterface, "httpTransport", httpTransport);
  }

  @Test
  public void uploadMediaContentAddsChecksumHeaderAndReturnsResponseBody() throws Exception {
    HttpResponse response = mock(HttpResponse.class);
    when(httpRequest.execute()).thenReturn(response);
    when(response.getStatusCode()).thenReturn(200);
    when(response.getContent())
        .thenReturn(new ByteArrayInputStream("upload-token".getBytes(StandardCharsets.UTF_8)));

    String actual =
        photosInterface.uploadMediaContent(
            new ByteArrayInputStream("test-bytes".getBytes(StandardCharsets.UTF_8)), SHA1);

    ArgumentCaptor<GenericUrl> urlCaptor = ArgumentCaptor.forClass(GenericUrl.class);
    verify(requestFactory).buildPostRequest(urlCaptor.capture(), any(HttpContent.class));
    assertTrue(urlCaptor.getValue().toString().contains("uploads/"));
    assertTrue(urlCaptor.getValue().toString().contains("access_token=" + ACCESS_TOKEN));
    assertTrue(urlCaptor.getValue().toString().contains("X-Goog-Upload-Protocol=raw"));
    assertTrue(urlCaptor.getValue().toString().contains("Content-type=application/octet-stream"));
    assertEquals("upload-token", actual);
    assertEquals(expectedHashHeader(SHA1), headers.getFirstHeaderStringValue("X-Goog-Hash"));
  }

  @Test
  public void uploadMediaContentConvertsChecksumMismatchToUploadError() throws Exception {
    HttpResponseException exception =
        new HttpResponseException.Builder(400, "Bad Request", new HttpHeaders())
            .setContent("User-provided checksum does not match received payload content.")
            .build();
    when(httpRequest.execute()).thenThrow(exception);

    UploadErrorException thrown =
        assertThrows(
            UploadErrorException.class,
            () ->
                photosInterface.uploadMediaContent(
                    new ByteArrayInputStream("test-bytes".getBytes(StandardCharsets.UTF_8)), SHA1));

    assertEquals(GooglePhotosInterface.ERROR_HASH_MISMATCH, thrown.getMessage());
  }

  private static void setField(Object target, String fieldName, Object value) throws Exception {
    Field field = GooglePhotosInterface.class.getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(target, value);
  }

  private static String expectedHashHeader(String sha1) {
    return "sha1=" + Base64.getEncoder().encodeToString(BaseEncoding.base16().decode(sha1.toUpperCase()));
  }
}