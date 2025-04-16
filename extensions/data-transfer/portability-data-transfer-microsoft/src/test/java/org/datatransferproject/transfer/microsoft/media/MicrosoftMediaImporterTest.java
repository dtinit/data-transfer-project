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

package org.datatransferproject.transfer.microsoft.media;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.truth.Truth.assertThat;
import static okhttp3.Protocol.HTTP_2;
import static org.datatransferproject.transfer.microsoft.MicrosoftApiResponse.CAUSE_PREFIX_UNRECOGNIZED_EXCEPTION;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.atLeast;
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
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.launcher.monitor.ConsoleMonitor;
import org.datatransferproject.spi.api.transport.JobFileStream;
import org.datatransferproject.spi.api.transport.RemoteFileStreamer;
import org.datatransferproject.spi.cloud.storage.TemporaryPerJobDataStore;
import org.datatransferproject.spi.cloud.storage.TemporaryPerJobDataStore.InputStreamWrapper;
import org.datatransferproject.spi.transfer.idempotentexecutor.IdempotentImportExecutor;
import org.datatransferproject.spi.transfer.idempotentexecutor.InMemoryIdempotentImportExecutor;
import org.datatransferproject.spi.transfer.provider.ImportResult;
import org.datatransferproject.spi.transfer.types.DestinationMemoryFullException;
import org.datatransferproject.spi.transfer.types.PermissionDeniedException;
import org.datatransferproject.transfer.microsoft.common.MicrosoftCredentialFactory;
import org.datatransferproject.types.common.DownloadableFile;
import org.datatransferproject.types.common.models.media.MediaAlbum;
import org.datatransferproject.types.common.models.media.MediaContainerResource;
import org.datatransferproject.types.common.models.photos.PhotoModel;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/**
 * This tests the MicrosoftMediaImporter. As of now, it only tests the number of requests called.
 * More tests are needed to test request bodies and parameters.
 */
public class MicrosoftMediaImporterTest {

  private static final int CHUNK_SIZE = 32000 * 1024; // 32000KiB
  private static final String BASE_URL = "https://www.baseurl.com";
  private static final UUID uuid = UUID.randomUUID();
  private static final String FAKE_ACCESS_TOKEN = "fake-acc-token-"+UUID.randomUUID();

  MicrosoftMediaImporter importer;
  OkHttpClient client = mock(OkHttpClient.class);
  OkHttpClient.Builder clientBuilder;
  ObjectMapper objectMapper;
  TemporaryPerJobDataStore jobStore;
  Monitor monitor;
  Credential credential;
  MicrosoftCredentialFactory credentialFactory;
  IdempotentImportExecutor executor;
  TokensAndUrlAuthData authData;
  RemoteFileStreamer remoteFileStreamer;

  @Before
  public void setUp() throws IOException {
    authData = mock(TokensAndUrlAuthData.class);
    clientBuilder = mock(OkHttpClient.Builder.class);
    doReturn(client).when(clientBuilder).build();
    objectMapper =
        new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    // mocked on a per test basis
    jobStore = mock(TemporaryPerJobDataStore.class);
    monitor = new ConsoleMonitor(ConsoleMonitor.Level.INFO);
    executor = new InMemoryIdempotentImportExecutor(monitor);
    credentialFactory = mock(MicrosoftCredentialFactory.class);
    credential = new Credential.Builder(BearerToken.authorizationHeaderAccessMethod()).build();
    RemoteFileStreamer remoteFileStreamer = mock(RemoteFileStreamer.class);
    when(credentialFactory.createCredential(any())).thenReturn(credential);
    when(credentialFactory.refreshCredential(any())).thenReturn(credential);
    credential.setAccessToken(FAKE_ACCESS_TOKEN);
    credential.setExpirationTimeMilliseconds(null);
    importer =
        new MicrosoftMediaImporter(
            BASE_URL,
            clientBuilder,
            objectMapper,
            jobStore,
            monitor,
            credentialFactory,
            new JobFileStream(remoteFileStreamer),
            1.0 /*maxWritesPerSecond*/);
  }

  @Test
  public void testCleanAlbumNames() throws Exception {
    List<MediaAlbum> albums =
        ImmutableList.of(new MediaAlbum("id1", "album1.", "This is a fake albumb"));

    MediaContainerResource data =
        new MediaContainerResource(albums, null /*phots*/, null /*videos*/);

    Call call = mock(Call.class);
    doReturn(call)
        .when(client)
        .newCall(
            argThat(
                (Request r) -> {
                  String body = "";

                  try {
                    final Buffer buffer = new Buffer();
                    r.body().writeTo(buffer);
                    body = buffer.readUtf8();
                  } catch (IOException e) {
                    return false;
                  }

                  return r.url()
                          .toString()
                          .equals("https://www.baseurl.com/v1.0/me/drive/special/photos/children")
                      && body.contains("album1_");
                }));
    Response response = fakeResponse(200, "OK", "{\"id\": \"id1\"}").build();
    when(call.execute()).thenReturn(response);

    ImportResult result = importer.importItem(uuid, executor, authData, data);
    verify(client, times(1)).newCall(any());
    assertThat(result).isEqualTo(ImportResult.OK);
  }

  @Test
  public void testImportItemPermissionDenied() throws Exception {
    List<MediaAlbum> albums =
        ImmutableList.of(new MediaAlbum("id1", "album1.", "This is a fake albumb"));

    MediaContainerResource data =
        new MediaContainerResource(albums, null /*photos*/, null /*videos*/);

    Call call = mock(Call.class);
    doReturn(call)
        .when(client)
        .newCall(
            argThat(
                (Request r) ->
                    r.url()
                        .toString()
                        .equals("https://www.baseurl.com/v1.0/me/drive/special/photos/children")));
    Response response = fakeResponse(403, "",
        "{" +
            "\"error\": {" +
                "\"code\":\"accessDenied\"," +
                "\"message\":\"Access Denied\"," +
                "\"localizedMessage\":\"アイテムが削除されているか、期限切れになっているか、またはこのアイテムへのアクセス許可がない可能性があります。詳細については、このアイテムの所有者に問い合わせてください。\"," +
                "\"innerError\": {\"date\":\"2024-12-24T01:03:02\",\"request-id\":\"fake-request-id\",\"client-request-id\":\"fake-client-request-id\"}" +
            "}" +
        "}"
        ).build();
    when(call.execute()).thenReturn(response);

    assertThrows(
        PermissionDeniedException.class,
        () -> {
          ImportResult result = importer.importItem(uuid, executor, authData, data);
        });
  }

  @Test
  public void testImportInsufficientStorage() throws Exception {
    List<MediaAlbum> albums =
        ImmutableList.of(new MediaAlbum("id1", "album1.", "This is a fake albumb"));

    MediaContainerResource data =
        new MediaContainerResource(albums, null /*photos*/, null /*videos*/);

    Call call = mock(Call.class);
    doReturn(call)
        .when(client)
        .newCall(
            argThat(
                (Request r) ->
                    r.url()
                        .toString()
                        .equals("https://www.baseurl.com/v1.0/me/drive/special/photos/children")));
    Response response =
        fakeErrorResponse(
                507, "" /*httpMessage*/, "{\"message\": \"Insufficient Space Available\"}")
            .build();
    when(call.execute()).thenReturn(response);

    assertThrows(
        DestinationMemoryFullException.class,
        () -> {
          ImportResult result = importer.importItem(uuid, executor, authData, data);
        });
  }

  @Test
  public void testImportUnrecognizedError() throws Exception {
    List<MediaAlbum> albums =
        ImmutableList.of(new MediaAlbum("id1", "album1.", "This is a fake albumb"));

    MediaContainerResource data =
        new MediaContainerResource(albums, null /*photos*/, null /*videos*/);

    Call call = mock(Call.class);
    doReturn(call)
        .when(client)
        .newCall(
            argThat(
                (Request r) ->
                    r.url()
                        .toString()
                        .equals("https://www.baseurl.com/v1.0/me/drive/special/photos/children")));
    Response response =
        fakeErrorResponse(
                500,
                "" /*httpMessage*/,
                "{\"message\": \"Hippo is the best dog, not a real-world response\"}")
            .build();
    when(call.execute()).thenReturn(response);

    ImportResult result = importer.importItem(uuid, executor, authData, data);
    assertThat(executor.getErrors().size()).isEqualTo(1);
    String exception =
        executor.getErrors().stream().map(e -> e.exception()).collect(Collectors.toList()).get(0);
    assertThat(exception).contains(CAUSE_PREFIX_UNRECOGNIZED_EXCEPTION);
  }

  private static <M extends DownloadableFile> M fakeJobstoreModel(
      TemporaryPerJobDataStore jobStore, M model, byte[] contents) throws IOException {
    checkState(
        model.isInTempStore(),
        "nonsensical fake: trying to fake a tempstore entry with isInTempStore() set to false");
    when(jobStore.getStream(uuid, model.getFetchableUrl()))
        .thenAnswer(
            new Answer() {
              public Object answer(InvocationOnMock invocation) {
                return new InputStreamWrapper(new ByteArrayInputStream(contents));
              }
            });
    return model;
  }

  @Test
  public void testImportItemAllSuccess() throws Exception {
    List<MediaAlbum> albums =
        ImmutableList.of(new MediaAlbum("id1", "albumb1", "This is a fake albumb"));

    Collection<PhotoModel> photos =
        ImmutableList.of(
            fakeJobstoreModel(
                jobStore,
                new PhotoModel(
                    "Pic1",
                    "http://fake.com/1.jpg",
                    "A pic",
                    "image/jpg",
                    "p1", // dataId
                    "id1", // albumdId
                    true /*isInTempStore*/),
                new byte[CHUNK_SIZE]),
            fakeJobstoreModel(
                jobStore,
                new PhotoModel(
                    "Pic2",
                    "http://fake.com/2.png",
                    "fine art",
                    "image/png",
                    "p2", // dataId
                    "id1", // albumdId
                    true /*isInTempStore*/),
                new byte[CHUNK_SIZE]));
    MediaContainerResource data = new MediaContainerResource(albums, photos, null /*videos*/);

    Call call = mock(Call.class);
    doReturn(call)
        .when(client)
        .newCall(
            argThat(
                (Request r) ->
                    r.url()
                        .toString()
                        .equals("https://www.baseurl.com/v1.0/me/drive/special/photos/children")));
    Response response = fakeResponse(200, "OK", "{\"id\": \"id1\"}").build();
    when(call.execute()).thenReturn(response);

    Call call2 = mock(Call.class);
    doReturn(call2)
        .when(client)
        .newCall(argThat((Request r) -> {
          return r.url().toString().contains("createUploadSession")
              && r.header("Authorization") != null
              && r.header("Authorization").contains("Bearer")
              && r.header("Authorization").contains(FAKE_ACCESS_TOKEN);
        }));
    Response response2 =
        fakeResponse(200, "OK", "{\"uploadUrl\": \"https://scalia.com/link\"}").build();
    when(call2.execute()).thenReturn(response2);

    Call call3 = mock(Call.class);
    doReturn(call3)
        .when(client)
        .newCall(argThat((Request r) -> {
          return r.url().toString().contains("scalia.com/link")
              // Regression coverage: we _don't_ want a token sent for every chunk.
              // https://github.com/dtinit/data-transfer-project/pull/1416
              && r.header("Authorization") == null;
        }));
    Response response3 = fakeResponse(200, "OK", "{\"id\": \"rand1\"}").build();
    when(call3.execute()).thenReturn(response3);

    ImportResult result = importer.importItem(uuid, executor, authData, data);
    verify(client, atLeast(albums.size() + photos.size())).newCall(any());
    assertThat(result).isEqualTo(ImportResult.OK);
  }

  private static Response.Builder fakeErrorResponse(
      int statusCode, String httpMessage, String jsonErrorValue) {
    return fakeResponse(statusCode, httpMessage, String.format("{ \"error\": %s ", jsonErrorValue));
  }

  private static Response.Builder fakeResponse(
      int statusCode, String httpMessage, String jsonBody) {
    Response.Builder builder = fakeResponse(statusCode, httpMessage);
    builder.body(ResponseBody.create(MediaType.parse("application/json"), jsonBody));
    return builder;
  }

  private static Response.Builder fakeResponse(int statusCode, String httpMessage) {
    Request fakeRequest = new Request.Builder().url("https://some/mock/url").build();
    return new Response.Builder()
        .request(fakeRequest)
        .protocol(HTTP_2)
        .code(statusCode)
        .message(httpMessage);
  }
}
