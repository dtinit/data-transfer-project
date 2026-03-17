/*
 * Copyright 2025 The Data Transfer Project Authors.
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
 *
 */

package org.datatransferproject.datatransfer.synology.service;

import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import okhttp3.Call;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.datatransfer.synology.service.SynologyDTPService.RequestBodyGenerator;
import org.datatransferproject.datatransfer.synology.utils.TestConfigs;
import org.datatransferproject.spi.cloud.storage.JobStore;
import org.datatransferproject.spi.cloud.storage.TemporaryPerJobDataStore.InputStreamWrapper;
import org.datatransferproject.spi.transfer.types.CopyExceptionWithFailureReason;
import org.datatransferproject.spi.transfer.types.DestinationMemoryFullException;
import org.datatransferproject.spi.transfer.types.InvalidTokenException;
import org.datatransferproject.spi.transfer.types.NoNasInAccountException;
import org.datatransferproject.spi.transfer.types.UploadErrorException;
import org.datatransferproject.types.common.DownloadableFile;
import org.datatransferproject.types.common.models.media.MediaAlbum;
import org.datatransferproject.types.common.models.photos.PhotoModel;
import org.datatransferproject.types.common.models.videos.VideoModel;
import org.datatransferproject.types.transfer.serviceconfig.TransferServiceConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SynologyDTPServiceTest {
  private final String exportingService = "mockService";
  private final UUID jobId = UUID.randomUUID();
  protected SynologyDTPService dtpService;
  @Mock protected Monitor monitor;
  @Mock protected TransferServiceConfig transferServiceConfig;
  @Mock protected JobStore jobStore;
  @Mock protected SynologyOAuthTokenManager tokenManager;
  @Captor ArgumentCaptor<RequestBodyGenerator> requestBodyCaptor;
  @Mock private OkHttpClient client;

  @BeforeEach
  public void setUp() throws InvalidTokenException {
    lenient().when(tokenManager.getAccessToken(jobId)).thenReturn("mockAccessToken");
    lenient()
        .when(transferServiceConfig.getServiceConfig())
        .thenReturn(TestConfigs.createServiceConfigJson());

    dtpService =
        new SynologyDTPService(
            monitor, transferServiceConfig, exportingService, jobStore, tokenManager, client);
  }

  @Nested
  public class AddItemToAlbum {
    private final String albumId = "testAlbum";
    private final String itemId = "testItem";

    @Test
    public void shouldSendPostRequestWithCorrectFormBody() throws CopyExceptionWithFailureReason {
      SynologyDTPService spyService = Mockito.spy(dtpService);

      doReturn(Map.of("success", true))
          .when(spyService)
          .sendPostRequest(anyString(), any(RequestBodyGenerator.class), any());
      Map<String, Object> result = spyService.addItemToAlbum(albumId, itemId, jobId);

      verify(spyService).sendPostRequest(anyString(), requestBodyCaptor.capture(), any());
      assertEquals(result.get("success"), true);

      RequestBody capturedBody = requestBodyCaptor.getValue().get();
      assertTrue(capturedBody instanceof FormBody);
      FormBody formBody = (FormBody) capturedBody;

      Map<String, String> formBodyMap =
          Map.of(
              "job_id", jobId.toString(),
              "service", exportingService,
              "album_id", albumId,
              "item_id", itemId);
      for (int i = 0; i < formBody.size(); i++) {
        assertEquals(formBodyMap.get(formBody.name(i)), formBody.value(i));
      }
    }

    @Test
    public void shouldThrowExceptionIfSendPostRequestFailed()
        throws CopyExceptionWithFailureReason {
      SynologyDTPService spyService = Mockito.spy(dtpService);

      doThrow(new UploadErrorException("MockException", null))
          .when(spyService)
          .sendPostRequest(anyString(), any(RequestBodyGenerator.class), any());

      assertThrows(
          UploadErrorException.class,
          () -> spyService.addItemToAlbum(albumId, itemId, jobId),
          "MockException");
    }
  }

  @Nested
  public class CreateAlbum {
    private final String albumId = "testAlbumId";
    private final String albumName = "testAlbumName";
    private final MediaAlbum album = new MediaAlbum(albumId, albumName, "");

    @Test
    public void shouldSendPostRequestWithCorrectFormBody() throws CopyExceptionWithFailureReason {
      SynologyDTPService spyService = Mockito.spy(dtpService);
      Map<String, Object> dataMap = Map.of("album_id", albumId);

      doReturn(Map.of("success", true, "data", dataMap))
          .when(spyService)
          .sendPostRequest(anyString(), any(RequestBodyGenerator.class), any());
      Map<String, Object> result = spyService.createAlbum(album, jobId);

      verify(spyService).sendPostRequest(anyString(), requestBodyCaptor.capture(), any());
      assertEquals(result.get("success"), null);
      assertEquals(result.get("album_id"), albumId);

      RequestBody capturedBody = requestBodyCaptor.getValue().get();
      assertTrue(capturedBody instanceof FormBody);
      FormBody formBody = (FormBody) capturedBody;

      Map<String, String> formBodyMap =
          Map.of(
              "job_id", jobId.toString(),
              "service", exportingService,
              "album_id", albumId,
              "title", albumName);
      for (int i = 0; i < formBody.size(); i++) {
        assertEquals(formBodyMap.get(formBody.name(i)), formBody.value(i));
      }
    }

    @Test
    public void shouldThrowExceptionIfSendPostRequestFailed()
        throws CopyExceptionWithFailureReason {
      SynologyDTPService spyService = Mockito.spy(dtpService);

      doThrow(new UploadErrorException("MockException", null))
          .when(spyService)
          .sendPostRequest(anyString(), any(RequestBodyGenerator.class), any());

      assertThrows(
          UploadErrorException.class, () -> spyService.createAlbum(album, jobId), "MockException");
    }
  }

  @Nested
  @TestInstance(Lifecycle.PER_CLASS)
  public class CreateMedia {
    private final String itemName = "itemName";
    private final String fetchUrl = "https://example.com";
    private final String description = "desc";
    private final String itemId = "itemId";
    private final Long uploadedTimestampInSeconds = 1718697600L; // 2024-06-18 00:00:00 UTC

    public Stream<Object> provideMediaObjectsInTempStore() {
      Date uploadedTime = new Date(uploadedTimestampInSeconds * 1000);
      return Stream.of(
          new PhotoModel(
              itemName, fetchUrl, description, "mediaType", itemId, null, true, uploadedTime),
          new VideoModel(
              itemName, fetchUrl, description, "format", itemId, null, true, uploadedTime));
    }

    public Stream<Object> provideMediaObjectsWithoutDescriptionAndUploadedTimeInTempStore() {
      return Stream.of(
          new PhotoModel(itemName, fetchUrl, null, "mediaType", itemId, null, true),
          new VideoModel(itemName, fetchUrl, null, "format", itemId, null, true, null));
    }

    public Stream<Object> provideMediaObjectsNotInTempStore() {
      return Stream.of(
          new PhotoModel(itemName, fetchUrl, description, "mediaType", itemId, null, false),
          new VideoModel(itemName, fetchUrl, description, "format", itemId, null, false, null));
    }

    @ParameterizedTest(
        name =
            "shouldSendPostRequestWithCorrectFormBodyWithDescriptionAndUploadedTime [{index}] {0}")
    @MethodSource("provideMediaObjectsInTempStore")
    public void shouldSendPostRequestWithCorrectFormBodyWithDescriptionAndUploadedTime(
        DownloadableFile item) throws IOException, CopyExceptionWithFailureReason {
      byte[] mockImage = new byte[] {1, 2, 3};
      InputStream mockInputStream = new ByteArrayInputStream(mockImage);
      InputStreamWrapper streamWrapper = mock(InputStreamWrapper.class);
      SynologyDTPService spyService = Mockito.spy(dtpService);
      Map<String, Object> dataMap = Map.of("item_id", itemId);

      when(jobStore.getStream(jobId, fetchUrl)).thenReturn(streamWrapper);
      when(streamWrapper.getStream()).thenReturn(mockInputStream);
      Map<String, Object> result = new HashMap();
      if (item instanceof PhotoModel) {
        doReturn(Map.of("success", true, "data", dataMap))
            .when(spyService)
            .sendPostRequest(anyString(), any(RequestBodyGenerator.class), any());
        result = spyService.createPhoto((PhotoModel) item, jobId);
        verify(spyService).sendPostRequest(anyString(), requestBodyCaptor.capture(), any());
      } else if (item instanceof VideoModel) {
        doReturn(Map.of("success", true, "data", dataMap))
            .when(spyService)
            .sendPostRequest(anyString(), any(RequestBodyGenerator.class), any(), anyInt());
        result = spyService.createVideo((VideoModel) item, jobId);
        verify(spyService)
            .sendPostRequest(anyString(), requestBodyCaptor.capture(), any(), anyInt());
      }

      assertEquals(result.get("item_id"), itemId);
      assertEquals(result.get("success"), null);

      RequestBody capturedBody = requestBodyCaptor.getValue().get();
      MultipartBody multipartBody = (MultipartBody) capturedBody;

      Map<String, String> multipartFormAnswer =
          Map.of(
              "job_id", jobId.toString(),
              "service", exportingService,
              "item_id", itemId,
              "title", itemName,
              "description", description,
              "uploaded_time", String.valueOf(uploadedTimestampInSeconds));

      for (MultipartBody.Part part : multipartBody.parts()) {
        String partName =
            part.headers().get("Content-Disposition").split(";")[1].split("=")[1].replace("\"", "");
        Buffer buffer = new Buffer();
        part.body().writeTo(buffer);
        if (partName.equals("file")) {
          byte[] partBytes = buffer.readByteArray();

          assertArrayEquals(mockImage, partBytes);
          String fileName = item.getName();
          String contentDisposition = part.headers().get("Content-Disposition");
          assertTrue(contentDisposition.contains("filename=\"" + fileName + "\""));
        } else if (multipartFormAnswer.containsKey(partName)) {
          String partValue = buffer.readUtf8();
          assertEquals(multipartFormAnswer.get(partName), partValue);
        }
      }
    }

    @ParameterizedTest(
        name =
            "shouldSendPostRequestWithCorrectFormBodyWithoutDescriptionAndUploadedTime [{index}]"
                + " {0}")
    @MethodSource("provideMediaObjectsWithoutDescriptionAndUploadedTimeInTempStore")
    public void shouldSendPostRequestWithCorrectFormBodyWithoutDescription(DownloadableFile item)
        throws IOException, CopyExceptionWithFailureReason {
      byte[] mockImage = new byte[] {1, 2, 3};
      InputStream mockInputStream = new ByteArrayInputStream(mockImage);
      InputStreamWrapper streamWrapper = mock(InputStreamWrapper.class);
      SynologyDTPService spyService = Mockito.spy(dtpService);
      Map<String, Object> dataMap = Map.of("item_id", itemId);

      when(jobStore.getStream(jobId, fetchUrl)).thenReturn(streamWrapper);
      when(streamWrapper.getStream()).thenReturn(mockInputStream);
      Map<String, Object> result = new HashMap();
      if (item instanceof PhotoModel) {
        doReturn(Map.of("success", true, "data", dataMap))
            .when(spyService)
            .sendPostRequest(anyString(), any(RequestBodyGenerator.class), any());
        result = spyService.createPhoto((PhotoModel) item, jobId);
        verify(spyService).sendPostRequest(anyString(), requestBodyCaptor.capture(), any());
      } else if (item instanceof VideoModel) {
        doReturn(Map.of("success", true, "data", dataMap))
            .when(spyService)
            .sendPostRequest(anyString(), any(RequestBodyGenerator.class), any(), anyInt());
        result = spyService.createVideo((VideoModel) item, jobId);
        verify(spyService)
            .sendPostRequest(anyString(), requestBodyCaptor.capture(), any(), anyInt());
      }

      assertEquals(result.get("item_id"), itemId);
      assertEquals(result.get("success"), null);

      RequestBody capturedBody = requestBodyCaptor.getValue().get();
      MultipartBody multipartBody = (MultipartBody) capturedBody;

      Map<String, String> multipartFormAnswer =
          Map.of(
              "job_id", jobId.toString(),
              "service", exportingService,
              "item_id", itemId,
              "title", itemName);

      for (MultipartBody.Part part : multipartBody.parts()) {
        String partName =
            part.headers().get("Content-Disposition").split(";")[1].split("=")[1].replace("\"", "");
        Buffer buffer = new Buffer();
        part.body().writeTo(buffer);
        assertNotEquals("description", partName);
        if (partName.equals("file")) {
          byte[] partBytes = buffer.readByteArray();

          assertArrayEquals(mockImage, partBytes);
          String fileName = item.getName();
          String contentDisposition = part.headers().get("Content-Disposition");
          assertTrue(contentDisposition.contains("filename=\"" + fileName + "\""));
        } else if (multipartFormAnswer.containsKey(partName)) {
          String partValue = buffer.readUtf8();
          assertEquals(multipartFormAnswer.get(partName), partValue);
        }
      }
    }

    @ParameterizedTest(name = "shouldThrowExceptionIfInputStreamIsConsumed [{index}] {0}")
    @MethodSource("provideMediaObjectsInTempStore")
    public void shouldThrowExceptionIfInputStreamIsConsumed(DownloadableFile item)
        throws IOException, CopyExceptionWithFailureReason {
      byte[] mockImage = new byte[] {1, 2, 3};
      InputStream mockInputStream = new ByteArrayInputStream(mockImage);
      InputStreamWrapper streamWrapper = mock(InputStreamWrapper.class);
      SynologyDTPService spyService = Mockito.spy(dtpService);
      Map<String, Object> dataMap = Map.of("item_id", itemId);

      when(jobStore.getStream(jobId, fetchUrl)).thenReturn(streamWrapper);
      when(streamWrapper.getStream()).thenReturn(mockInputStream);
      if (item instanceof PhotoModel) {
        doReturn(Map.of("success", true, "data", dataMap))
            .when(spyService)
            .sendPostRequest(anyString(), any(RequestBodyGenerator.class), any());
        spyService.createPhoto((PhotoModel) item, jobId);
        verify(spyService).sendPostRequest(anyString(), requestBodyCaptor.capture(), any());
      } else if (item instanceof VideoModel) {
        doReturn(Map.of("success", true, "data", dataMap))
            .when(spyService)
            .sendPostRequest(anyString(), any(RequestBodyGenerator.class), any(), anyInt());
        spyService.createVideo((VideoModel) item, jobId);
        verify(spyService)
            .sendPostRequest(anyString(), requestBodyCaptor.capture(), any(), anyInt());
      }

      RequestBody capturedBody = requestBodyCaptor.getValue().get();
      Buffer buffer = new Buffer();
      capturedBody.writeTo(buffer);
      IOException exception = assertThrows(IOException.class, () -> capturedBody.writeTo(buffer));
      assertTrue(exception.getMessage().contains("InputStream has already been consumed"));
    }

    @ParameterizedTest(name = "shouldThrowExceptionIfSendPostRequestFailed [{index}] {0}")
    @MethodSource("provideMediaObjectsInTempStore")
    public void shouldThrowExceptionIfSendPostRequestFailed(DownloadableFile item)
        throws IOException, CopyExceptionWithFailureReason {
      byte[] mockImage = new byte[] {1, 2, 3};
      InputStream mockInputStream = new ByteArrayInputStream(mockImage);
      InputStreamWrapper streamWrapper = mock(InputStreamWrapper.class);
      SynologyDTPService spyService = Mockito.spy(dtpService);

      if (item instanceof PhotoModel) {
        doThrow(new UploadErrorException("MockException", null))
            .when(spyService)
            .sendPostRequest(anyString(), any(RequestBodyGenerator.class), any());
        assertThrows(
            UploadErrorException.class,
            () -> spyService.createPhoto((PhotoModel) item, jobId),
            "MockException");
      } else if (item instanceof VideoModel) {
        doThrow(new UploadErrorException("MockException", null))
            .when(spyService)
            .sendPostRequest(anyString(), any(RequestBodyGenerator.class), any(), anyInt());
        assertThrows(
            UploadErrorException.class,
            () -> spyService.createVideo((VideoModel) item, jobId),
            "MockException");
      }
    }
  }

  @Nested
  public class SendPostRequestTest {
    private RequestBody requestBody = RequestBody.create(MediaType.parse("application/json"), "{}");

    @Test
    public void shouldRetryIfGotError() throws IOException, CopyExceptionWithFailureReason {
      Call mockCall = mock(Call.class);

      Response mockResponseFail = mock(Response.class);
      when(mockResponseFail.code()).thenReturn(SC_INTERNAL_SERVER_ERROR);
      when(mockResponseFail.isSuccessful()).thenReturn(false);

      Response mockResponseSuccess = mock(Response.class);
      ResponseBody mockResponseSuccessBody = mock(ResponseBody.class);
      when(mockResponseSuccess.isSuccessful()).thenReturn(true);
      when(mockResponseSuccess.body()).thenReturn(mockResponseSuccessBody);
      when(mockResponseSuccessBody.string()).thenReturn("{\"success\": true}");

      when(client.newCall(any(Request.class))).thenReturn(mockCall);
      when(mockCall.execute()).thenReturn(mockResponseFail).thenReturn(mockResponseSuccess);

      Map<String, Object> result =
          dtpService.sendPostRequest(TestConfigs.TEST_C2_BASE_URL, () -> requestBody, jobId);

      assertEquals(Map.of("success", true), result);

      verify(tokenManager, never())
          .refreshToken(any(UUID.class), eq(client), any(ObjectMapper.class));
      verify(client, times(2)).newCall(any(Request.class));
    }

    @Test
    public void shouldThrowExceptionIfReachMaxRetries() throws IOException {
      Call mockCall = mock(Call.class);

      Response mockResponseFail = mock(Response.class);
      when(mockResponseFail.code()).thenReturn(SC_INTERNAL_SERVER_ERROR);
      when(mockResponseFail.isSuccessful()).thenReturn(false);

      when(client.newCall(any(Request.class))).thenReturn(mockCall);
      when(mockCall.execute()).thenReturn(mockResponseFail);

      assertThrows(
          UploadErrorException.class,
          () -> dtpService.sendPostRequest(TestConfigs.TEST_C2_BASE_URL, () -> requestBody, jobId),
          String.format("Failed to send POST request %d times", TestConfigs.TEST_MAX_ATTEMPTS));
      verify(tokenManager, never())
          .refreshToken(any(UUID.class), eq(client), any(ObjectMapper.class));
      verify(client, times(TestConfigs.TEST_MAX_ATTEMPTS)).newCall(any(Request.class));
    }

    @Test
    public void shouldRefreshTokenAndRetryIfGotUnauthorizedHttpError()
        throws IOException, JsonProcessingException, CopyExceptionWithFailureReason {
      Call mockCall = mock(Call.class);

      Response mockResponseFail = mock(Response.class);
      when(mockResponseFail.code()).thenReturn(SC_UNAUTHORIZED);
      when(mockResponseFail.isSuccessful()).thenReturn(false);

      Response mockResponseSuccess = mock(Response.class);
      ResponseBody mockResponseSuccessBody = mock(ResponseBody.class);
      when(mockResponseSuccess.isSuccessful()).thenReturn(true);
      when(mockResponseSuccess.body()).thenReturn(mockResponseSuccessBody);
      when(mockResponseSuccessBody.string()).thenReturn("{\"success\": true}");

      when(client.newCall(any(Request.class))).thenReturn(mockCall);
      when(mockCall.execute()).thenReturn(mockResponseFail).thenReturn(mockResponseSuccess);
      when(tokenManager.refreshToken(any(UUID.class), eq(client), any(ObjectMapper.class)))
          .thenReturn(true);

      Map<String, Object> result =
          dtpService.sendPostRequest(TestConfigs.TEST_C2_BASE_URL, () -> requestBody, jobId);

      assertEquals(Map.of("success", true), result);

      verify(tokenManager, times(1))
          .refreshToken(any(UUID.class), eq(client), any(ObjectMapper.class));
      verify(client, times(2)).newCall(any(Request.class));
    }

    @Test
    public void shouldInvokeRefreshTokenOnlyOnceIfGotUnauthorizedMultipleTimes()
        throws IOException {
      Call mockCall = mock(Call.class);

      Response mockResponseFail = mock(Response.class);
      when(mockResponseFail.code()).thenReturn(SC_UNAUTHORIZED);
      when(mockResponseFail.isSuccessful()).thenReturn(false);

      when(client.newCall(any(Request.class))).thenReturn(mockCall);
      when(mockCall.execute()).thenReturn(mockResponseFail);
      when(tokenManager.refreshToken(any(UUID.class), eq(client), any(ObjectMapper.class)))
          .thenReturn(false);

      assertThrows(
          UploadErrorException.class,
          () -> dtpService.sendPostRequest(TestConfigs.TEST_C2_BASE_URL, () -> requestBody, jobId),
          String.format("Failed to send POST request %d times", TestConfigs.TEST_MAX_ATTEMPTS));

      verify(tokenManager, times(1))
          .refreshToken(any(UUID.class), eq(client), any(ObjectMapper.class));
      verify(client, times(TestConfigs.TEST_MAX_ATTEMPTS)).newCall(any(Request.class));
    }

    @Test
    public void shouldThrowExceptionIfParseResponseFailed() throws IOException {
      Response mockResponse = mock(Response.class);
      ResponseBody mockResponseBody = mock(ResponseBody.class);
      Call mockCall = mock(Call.class);

      when(client.newCall(any(Request.class))).thenReturn(mockCall);
      when(mockCall.execute()).thenReturn(mockResponse);
      when(mockResponse.isSuccessful()).thenReturn(true);
      when(mockResponse.body()).thenReturn(mockResponseBody);
      when(mockResponseBody.string())
          .thenThrow(new IOException("Error when call response.body.string()"));

      assertThrows(
          UploadErrorException.class,
          () -> dtpService.sendPostRequest(TestConfigs.TEST_C2_BASE_URL, () -> requestBody, jobId),
          String.format("Failed to send POST request %d times", TestConfigs.TEST_MAX_ATTEMPTS));
      verify(tokenManager, never())
          .refreshToken(any(UUID.class), eq(client), any(ObjectMapper.class));
      verify(client, times(TestConfigs.TEST_MAX_ATTEMPTS)).newCall(any(Request.class));
    }

    @Test
    public void shouldReturnResponseData()
        throws IOException, JsonProcessingException, CopyExceptionWithFailureReason {
      Response mockResponse = mock(Response.class);
      ResponseBody mockResponseBody = mock(ResponseBody.class);
      Call mockCall = mock(Call.class);

      when(client.newCall(any(Request.class))).thenReturn(mockCall);
      when(mockCall.execute()).thenReturn(mockResponse);
      when(mockResponse.isSuccessful()).thenReturn(true);
      when(mockResponse.body()).thenReturn(mockResponseBody);
      when(mockResponseBody.string()).thenReturn("{\"success\": true}");

      Map<String, Object> result =
          dtpService.sendPostRequest(TestConfigs.TEST_C2_BASE_URL, () -> requestBody, jobId);
      assertEquals(Map.of("success", true), result);
      verify(tokenManager, never())
          .refreshToken(any(UUID.class), eq(client), any(ObjectMapper.class));
      verify(client, times(1)).newCall(any(Request.class));
    }

    @Test
    public void shouldThrowExceptionIfGot413() throws IOException {
      Call mockCall = mock(Call.class);

      Response mockResponseFail = mock(Response.class);
      when(mockResponseFail.code()).thenReturn(413);
      when(mockResponseFail.isSuccessful()).thenReturn(false);

      when(client.newCall(any(Request.class))).thenReturn(mockCall);
      when(mockCall.execute()).thenReturn(mockResponseFail);

      assertThrows(
          DestinationMemoryFullException.class,
          () -> dtpService.sendPostRequest(TestConfigs.TEST_C2_BASE_URL, () -> requestBody, jobId));
    }

    @Test
    public void shouldCallCheckUnprocessableContentIfGot422()
        throws IOException, CopyExceptionWithFailureReason {
      Call mockCall = mock(Call.class);
      SynologyDTPService spyService = Mockito.spy(dtpService);

      Response mockResponseFail = mock(Response.class);
      ResponseBody mockResponseBody = mock(ResponseBody.class);
      when(mockResponseFail.code()).thenReturn(422);
      when(mockResponseFail.isSuccessful()).thenReturn(false);
      when(mockResponseFail.body()).thenReturn(mockResponseBody);
      when(mockResponseBody.string())
          .thenReturn(
              "{\"error\":{\"code\":2000,\"message\":\"User has not claimed C2 Storage"
                  + " quota.\"}}");

      when(client.newCall(any(Request.class))).thenReturn(mockCall);
      when(mockCall.execute()).thenReturn(mockResponseFail);

      assertThrows(
          NoNasInAccountException.class,
          () -> spyService.sendPostRequest(TestConfigs.TEST_C2_BASE_URL, () -> requestBody, jobId));
    }
  }

  @Nested
  public class CheckUnprocessableContentTest {
    private Response mockResponse(String jsonBody) {
      ResponseBody body = ResponseBody.create(MediaType.parse("application/json"), jsonBody);
      Response response = mock(Response.class);
      when(response.body()).thenReturn(body);
      return response;
    }

    @Test
    public void shouldThrowExceptionIfErrorCodeIs2000() throws CopyExceptionWithFailureReason {
      String jsonBody =
          "{\"error\":{\"code\": 2000,\"message\":\"User has not claimed C2 Storage quota.\"}}";
      Response response = mockResponse(jsonBody);

      assertThrows(
          NoNasInAccountException.class, () -> dtpService.throwExceptionIfNoQuota(response));
    }

    @Test
    public void shouldThrowExceptionIfErrorCodeIs2001() throws CopyExceptionWithFailureReason {
      String jsonBody =
          "{\"error\":{\"code\": 2001,\"message\":\"C2 Storage quota is not enough.\"}}";
      Response response = mockResponse(jsonBody);

      assertThrows(
          NoNasInAccountException.class, () -> dtpService.throwExceptionIfNoQuota(response));
    }

    @Test
    public void shouldNotThrowExceptionIfNoErrorCode() throws CopyExceptionWithFailureReason {
      String jsonBody = "{\"data\":{\"album_id\":\"test_album_id\"}}";
      Response response = mockResponse(jsonBody);
      dtpService.throwExceptionIfNoQuota(response);
    }

    @Test
    public void shouldNotThrowExceptionIfErrorCodeIsDifferent()
        throws CopyExceptionWithFailureReason {
      String jsonBody = "{\"error\":{\"code\":\"1000\",\"message\":\"Some other error.\"}}";
      Response response = mockResponse(jsonBody);
      dtpService.throwExceptionIfNoQuota(response);
    }

    @Test
    public void shouldNotThrowExceptionIfBodyIsInvalidJson() throws CopyExceptionWithFailureReason {
      String jsonBody = "invalid-json";
      Response response = mockResponse(jsonBody);
      dtpService.throwExceptionIfNoQuota(response);
    }
  }

  @Nested
  public class GetMediaInputStreamWrapperTest {
    @Test
    public void shouldGetStreamFromJobStore() throws CopyExceptionWithFailureReason, IOException {
      // setup
      String fetchableUrl = "some_key";
      InputStream inputStream = new ByteArrayInputStream("test data".getBytes());
      long size = 100L;
      InputStreamWrapper expected = new InputStreamWrapper(inputStream, size);
      when(jobStore.getStream(jobId, fetchableUrl)).thenReturn(expected);

      // act
      InputStreamWrapper result = dtpService.getMediaInputStreamWrapper(jobId, fetchableUrl, true);

      // assert
      assertEquals(expected, result);
    }

    @Test
    public void shouldGetStreamFromUrl() throws Exception {
      // setup
      Path tempFile = Files.createTempFile("test", ".txt");
      tempFile.toFile().deleteOnExit();
      byte[] testData = "test data".getBytes();
      Files.write(tempFile, testData);
      String fetchableUrl = tempFile.toUri().toURL().toString();

      // act
      InputStreamWrapper result = dtpService.getMediaInputStreamWrapper(jobId, fetchableUrl, false);

      // assert
      assertEquals(testData.length, result.getBytes());
      try (InputStream is = result.getStream()) {
        byte[] bytes = new byte[is.available()];
        is.read(bytes);
        assertArrayEquals(testData, bytes);
      }
    }

    @Test
    public void shouldThrowExceptionWhenNoSource() {
      UploadErrorException exception =
          assertThrows(
              UploadErrorException.class,
              () -> dtpService.getMediaInputStreamWrapper(jobId, null, false));

      assertEquals("No valid input stream source for media", exception.getMessage());
    }

    @Test
    public void shouldThrowExceptionForMalformedUrl() {
      String malformedUrl = "this is not a url";
      UploadErrorException exception =
          assertThrows(
              UploadErrorException.class,
              () -> dtpService.getMediaInputStreamWrapper(jobId, malformedUrl, false));
      assertEquals(
          "Failed to create url for fetchableUrl [this is not a url]", exception.getMessage());
    }
  }
}
