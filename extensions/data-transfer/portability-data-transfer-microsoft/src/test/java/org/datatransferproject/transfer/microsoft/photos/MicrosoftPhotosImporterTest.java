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

package org.datatransferproject.transfer.microsoft.photos;

import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import com.google.api.client.auth.oauth2.Credential;

import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.spi.transfer.idempotentexecutor.IdempotentImportExecutor;
import org.datatransferproject.launcher.monitor.ConsoleMonitor;
import org.datatransferproject.spi.cloud.storage.TemporaryPerJobDataStore;
import org.datatransferproject.spi.transfer.provider.ImportResult;
import org.datatransferproject.spi.transfer.types.ContinuationData;
import org.datatransferproject.transfer.microsoft.common.MicrosoftCredentialFactory;
import org.datatransferproject.transfer.microsoft.driveModels.*;
import org.datatransferproject.types.common.StringPaginationToken;
import org.datatransferproject.types.common.models.ContainerResource;
import org.datatransferproject.types.common.models.IdOnlyContainerResource;
import org.datatransferproject.types.common.models.photos.PhotoAlbum;
import org.datatransferproject.types.common.models.photos.PhotoModel;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;
import org.datatransferproject.types.common.models.photos.PhotosContainerResource;
import org.datatransferproject.transfer.microsoft.DataChunk;
import org.datatransferproject.test.types.FakeIdempotentImportExecutor;
import org.junit.Before;
import com.google.api.client.auth.oauth2.Credential;

import com.google.api.client.auth.oauth2.BearerToken;

import org.junit.Test;
import org.mockito.Matchers;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.Call;

import com.fasterxml.jackson.databind.DeserializationFeature;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;
import com.google.common.collect.ImmutableList;


import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


/** 
  This tests the MicrosoftPhotosImporter. As of now, it only tests the number
  of requests called. More tests are needed to test request bodies and parameters.
*/
public class MicrosoftPhotosImporterTest {

  private static final int CHUNK_SIZE = 32000 * 1024; // 32000KiB
  private final static String BASE_URL = "https://www.baseurl.com";
  private final static UUID uuid = UUID.randomUUID();

  MicrosoftPhotosImporter importer;
  OkHttpClient client;
  ObjectMapper objectMapper;
  TemporaryPerJobDataStore jobStore;
  Monitor monitor;
  Credential credential;
  MicrosoftCredentialFactory credentialFactory;
  IdempotentImportExecutor executor;
  TokensAndUrlAuthData authData;


  @Before
  public void setUp() throws IOException {
    executor = new FakeIdempotentImportExecutor();
    authData = mock(TokensAndUrlAuthData.class);
    client =  mock(OkHttpClient.class);
    objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    // mocked on a per test basis
    jobStore = mock(TemporaryPerJobDataStore.class);
    monitor = new ConsoleMonitor(ConsoleMonitor.Level.INFO);
    credentialFactory = mock(MicrosoftCredentialFactory.class);
    credential = new Credential.Builder(BearerToken.authorizationHeaderAccessMethod()).build();
    when(credentialFactory.createCredential(any())).thenReturn(credential);
    when(credentialFactory.refreshCredential(any())).thenReturn(credential);
    credential.setAccessToken("acc");
    credential.setExpirationTimeMilliseconds(null);
    importer = new MicrosoftPhotosImporter(
      BASE_URL,
      client,
      objectMapper,
      jobStore,
      monitor,
      credentialFactory
    );
  }

  @Test
  public void testImportItemAllSuccess() throws Exception {
    List<PhotoAlbum> albums =
      ImmutableList.of(new PhotoAlbum("id1", "albumb1", "This is a fake albumb"));

    List<PhotoModel> photos =
      ImmutableList.of(
        new PhotoModel(
          "Pic1", "http://fake.com/1.jpg", "A pic", "image/jpg", "p1", "id1",
          true),
        new PhotoModel(
          "Pic2", "https://fake.com/2.png", "fine art", "image/png", "p2", "id1", true));
    when(jobStore.getStream(uuid, "http://fake.com/1.jpg"))
    .thenReturn(new ByteArrayInputStream(new byte[CHUNK_SIZE]));
    when(jobStore.getStream(uuid, "https://fake.com/2.png"))
    .thenReturn(new ByteArrayInputStream(new byte[CHUNK_SIZE]));
    PhotosContainerResource data = new PhotosContainerResource(albums, photos);

    Call call = mock(Call.class);
    doReturn(call).when(client).newCall(argThat((Request r) ->
                                        r.url().toString().equals("https://www.baseurl.com/v1.0/me/drive/special/photos/children")));
    Response response = mock(Response.class);
    ResponseBody body = mock(ResponseBody.class);
    when(body.bytes()).thenReturn(ResponseBody.create(MediaType.parse("application/json"),  "{\"id\": \"id1\"}").bytes());
    when(body.string()).thenReturn(ResponseBody.create(MediaType.parse("application/json"),  "{\"id\": \"id1\"}").string());
    when(response.code()).thenReturn(200);
    when(response.body()).thenReturn(body);
    when(call.execute()).thenReturn(response);

    Call call2 = mock(Call.class);
    doReturn(call2).when(client).newCall(argThat((Request r) ->
                                         r.url().toString().contains("createUploadSession")));
    Response response2 = mock(Response.class);
    ResponseBody body2 = mock(ResponseBody.class);
    when(body2.bytes()).thenReturn(ResponseBody.create(MediaType.parse("application/json"), "{\"uploadUrl\": \"https://scalia.com/link\"}").bytes());
    when(body2.string()).thenReturn(ResponseBody.create(MediaType.parse("application/json"), "{\"uploadUrl\": \"https://scalia.com/link\"}").string());
    when(response2.code()).thenReturn(200);
    when(response2.body()).thenReturn(body2);
    when(call2.execute()).thenReturn(response2);

    Call call3 = mock(Call.class);
    doReturn(call3).when(client).newCall(argThat((Request r) ->
                                         r.url().toString().contains("scalia.com/link")));
    Response response3 = mock(Response.class);
    ResponseBody body3 = mock(ResponseBody.class);
    when(body3.bytes()).thenReturn(ResponseBody.create(MediaType.parse("application/json"),  "{\"id\": \"rand1\"}").bytes());
    when(body3.string()).thenReturn(ResponseBody.create(MediaType.parse("application/json"),  "{\"id\": \"rand1\"}").string());
    when(response3.code()).thenReturn(200);
    when(response3.body()).thenReturn(body3);
    when(call3.execute()).thenReturn(response3);

    ImportResult result = importer.importItem(uuid, executor, authData, data);
    verify(client, times(5)).newCall(any());
    assertThat(result).isEqualTo(ImportResult.OK);
  }
}
