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

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.Credential;
import com.google.common.collect.ImmutableList;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.launcher.monitor.ConsoleMonitor;
import org.datatransferproject.spi.cloud.storage.TemporaryPerJobDataStore;
import org.datatransferproject.spi.cloud.storage.TemporaryPerJobDataStore.InputStreamWrapper;
import org.datatransferproject.spi.transfer.idempotentexecutor.IdempotentImportExecutor;
import org.datatransferproject.spi.transfer.provider.ImportResult;
import org.datatransferproject.spi.transfer.types.PermissionDeniedException;
import org.datatransferproject.test.types.FakeIdempotentImportExecutor;
import org.datatransferproject.transfer.microsoft.common.MicrosoftCredentialFactory;
import org.datatransferproject.types.common.models.photos.PhotoAlbum;
import org.datatransferproject.types.common.models.photos.PhotoModel;
import org.datatransferproject.types.common.models.photos.PhotosContainerResource;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;
import org.junit.Before;
import org.junit.Test;


/**
 * This tests the MicrosoftPhotosImporter. As of now, it only tests the number of requests called.
 * More tests are needed to test request bodies and parameters.
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
    client = mock(OkHttpClient.class);
    objectMapper = new ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
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
  public void testCleanAlbumNames() throws Exception {
    List<PhotoAlbum> albums =
        ImmutableList.of(new PhotoAlbum("id1", "album1.", "This is a fake albumb"));

    PhotosContainerResource data = new PhotosContainerResource(albums, null);

    Call call = mock(Call.class);
    doReturn(call).when(client).newCall(argThat((Request r) -> {
      String body = "";

      try {
        final Buffer buffer = new Buffer();
        r.body().writeTo(buffer);
        body = buffer.readUtf8();
      } catch (IOException e) {
        return false;
      }

      return
          r.url().toString().equals("https://www.baseurl.com/v1.0/me/drive/special/photos/children")
              &&
              body.contains("album1_");
    }));
    Response response = mock(Response.class);
    ResponseBody body = mock(ResponseBody.class);
    when(body.bytes()).thenReturn(
        ResponseBody.create(MediaType.parse("application/json"), "{\"id\": \"id1\"}").bytes());
    when(body.string()).thenReturn(
        ResponseBody.create(MediaType.parse("application/json"), "{\"id\": \"id1\"}").string());
    when(response.code()).thenReturn(200);
    when(response.body()).thenReturn(body);
    when(call.execute()).thenReturn(response);

    ImportResult result = importer.importItem(uuid, executor, authData, data);
    verify(client, times(1)).newCall(any());
    assertThat(result).isEqualTo(ImportResult.OK);
  }

  @Test
  public void testImportItemPermissionDenied() throws Exception {
    List<PhotoAlbum> albums =
        ImmutableList.of(new PhotoAlbum("id1", "album1.", "This is a fake albumb"));

    PhotosContainerResource data = new PhotosContainerResource(albums, null);

    Call call = mock(Call.class);
    doReturn(call).when(client).newCall(argThat((Request r) ->
        r.url().toString().equals("https://www.baseurl.com/v1.0/me/drive/special/photos/children")
    ));
    Response response = mock(Response.class);
    ResponseBody body = mock(ResponseBody.class);
    when(body.bytes()).thenReturn(
        ResponseBody.create(MediaType.parse("application/json"), "{\"id\": \"id1\"}").bytes());
    when(body.string()).thenReturn(
        ResponseBody.create(MediaType.parse("application/json"), "{\"id\": \"id1\"}").string());
    when(response.code()).thenReturn(403);
    when(response.message()).thenReturn("Access Denied");
    when(response.body()).thenReturn(body);
    when(call.execute()).thenReturn(response);

    assertThrows(PermissionDeniedException.class, () -> {
      ImportResult result = importer.importItem(uuid, executor, authData, data);
    });
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
        .thenReturn(new InputStreamWrapper(new ByteArrayInputStream(new byte[CHUNK_SIZE])));
    when(jobStore.getStream(uuid, "https://fake.com/2.png"))
        .thenReturn(new InputStreamWrapper(new ByteArrayInputStream(new byte[CHUNK_SIZE])));
    PhotosContainerResource data = new PhotosContainerResource(albums, photos);

    Call call = mock(Call.class);
    doReturn(call).when(client).newCall(argThat((Request r) ->
        r.url().toString()
            .equals("https://www.baseurl.com/v1.0/me/drive/special/photos/children")));
    Response response = mock(Response.class);
    ResponseBody body = mock(ResponseBody.class);
    when(body.bytes()).thenReturn(
        ResponseBody.create(MediaType.parse("application/json"), "{\"id\": \"id1\"}").bytes());
    when(body.string()).thenReturn(
        ResponseBody.create(MediaType.parse("application/json"), "{\"id\": \"id1\"}").string());
    when(response.code()).thenReturn(200);
    when(response.body()).thenReturn(body);
    when(call.execute()).thenReturn(response);

    Call call2 = mock(Call.class);
    doReturn(call2).when(client).newCall(argThat((Request r) ->
        r.url().toString().contains("createUploadSession")));
    Response response2 = mock(Response.class);
    ResponseBody body2 = mock(ResponseBody.class);
    when(body2.bytes()).thenReturn(ResponseBody
        .create(MediaType.parse("application/json"), "{\"uploadUrl\": \"https://scalia.com/link\"}")
        .bytes());
    when(body2.string()).thenReturn(ResponseBody
        .create(MediaType.parse("application/json"), "{\"uploadUrl\": \"https://scalia.com/link\"}")
        .string());
    when(response2.code()).thenReturn(200);
    when(response2.body()).thenReturn(body2);
    when(call2.execute()).thenReturn(response2);

    Call call3 = mock(Call.class);
    doReturn(call3).when(client).newCall(argThat((Request r) ->
        r.url().toString().contains("scalia.com/link")));
    Response response3 = mock(Response.class);
    ResponseBody body3 = mock(ResponseBody.class);
    when(body3.bytes()).thenReturn(
        ResponseBody.create(MediaType.parse("application/json"), "{\"id\": \"rand1\"}").bytes());
    when(body3.string()).thenReturn(
        ResponseBody.create(MediaType.parse("application/json"), "{\"id\": \"rand1\"}").string());
    when(response3.code()).thenReturn(200);
    when(response3.body()).thenReturn(body3);
    when(call3.execute()).thenReturn(response3);

    ImportResult result = importer.importItem(uuid, executor, authData, data);
    verify(client, times(5)).newCall(any());
    assertThat(result).isEqualTo(ImportResult.OK);
  }
}
