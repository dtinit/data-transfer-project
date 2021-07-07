/*
 * Copyright 2021 The Data Transfer Project Authors.
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

package org.datatransferproject.transfer.photobucket.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.cloud.local.LocalJobStore;
import org.datatransferproject.spi.cloud.storage.JobStore;
import org.datatransferproject.transfer.photobucket.data.PhotobucketConstants;
import org.datatransferproject.transfer.photobucket.model.ProcessingResult;
import org.datatransferproject.types.common.models.photos.PhotoAlbum;
import org.datatransferproject.types.common.models.photos.PhotoModel;
import org.datatransferproject.types.transfer.auth.AppCredentials;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.Date;
import java.util.UUID;

import static org.datatransferproject.transfer.photobucket.data.PhotobucketConstants.ALBUM_TITLE_PREFIX;
import static org.datatransferproject.transfer.photobucket.data.PhotobucketConstants.MAIN_PHOTO_ALBUM_TITLE_SUFFIX;
import static org.mockito.Mockito.*;

public class PhotobucketClientTest {
  private final Monitor monitor = mock(Monitor.class);
  private final UUID jobId = UUID.randomUUID();
  private final JobStore jobStore = new LocalJobStore();
  private final OkHttpClient httpClient = mock(OkHttpClient.class);
  private final ObjectMapper objectMapper = new ObjectMapper();
  HttpTransport httpTransport = mock(HttpTransport.class);
  JsonFactory jsonFactory = mock(JsonFactory.class);
  AppCredentials appCredentials = new AppCredentials("", "");
  TokensAndUrlAuthData tokensAndUrlAuthData =
      new TokensAndUrlAuthData("access", "refresh", "https://");

  @Test
  public void testCreateAlbumStructureAndUploadPhoto() throws Exception {
    PhotobucketConstants.IS_OVER_STORAGE_VERIFICATION_ENABLED = true;
    String EXTERNAl_NESTED_ALBUM_ID = UUID.randomUUID().toString();
    String ROOT_ALBUM_PB_ID = UUID.randomUUID().toString();
    String TOP_ALBUM_PB_ID = UUID.randomUUID().toString();
    String NESTED_ALBUM_PB_ID = UUID.randomUUID().toString();
    String IMAGE_PB_ID = UUID.randomUUID().toString();
    String REQUESTER = "Test run";
    PhotoAlbum nestedAlbum =
        new PhotoAlbum(
            EXTERNAl_NESTED_ALBUM_ID, "Some album under top level album", "Album description");
    PhotoModel photoModel =
        new PhotoModel(
            "title",
            "https://app.photobucket.com/img/home/inset-quality.jpg",
            "description",
            "mediaType",
            "dataId",
            EXTERNAl_NESTED_ALBUM_ID,
            false,
            new Date());

    Call call = mock(Call.class);
    Response response = mock(Response.class);
    ResponseBody responseBody = mock(ResponseBody.class);

    when(httpClient.newCall(any())).thenReturn(call);
    when(call.execute()).thenReturn(response);
    when(response.body()).thenReturn(responseBody);
    when(response.code())
        .thenAnswer(
            new Answer() {
              private int count = 0;

              public Object answer(InvocationOnMock invocation) {
                if (count == 4) {
                  count++;
                  return 201;
                } else {
                  count++;
                  return 200;
                }
              }
            });
    when(responseBody.string())
        .thenAnswer(
            new Answer() {
              private int count = 0;

              public Object answer(InvocationOnMock invocation) {
                switch (count) {
                  case 0:
                    count++;
                    return String.format(
                        "{\"data\":{\"getProfile\":{\"defaultAlbum\":\"%s\"}},\"errors\":[]}",
                        ROOT_ALBUM_PB_ID);
                  case 1:
                    count++;
                    return String.format(
                        "{\"data\":{\"createAlbum\":{\"id\":\"%s\"}},\"errors\":[]}",
                        TOP_ALBUM_PB_ID);
                  case 2:
                    count++;
                    return String.format(
                        "{\"data\":{\"createAlbum\":{\"id\":\"%s\"}},\"errors\":[]}",
                        NESTED_ALBUM_PB_ID);
                  case 3:
                    count++;
                    return "{\"availableSpace\": \"100000000\", \"availableImages\": \"1\"}";
                  case 4:
                    count++;
                    return String.format("{\"id\": \"%s\"}", IMAGE_PB_ID);
                  default:
                    return "{\"data\": null}";
                }
              }
            });

    PhotobucketCredentialsFactory photobucketCredentialsFactory =
        new PhotobucketCredentialsFactory(httpTransport, jsonFactory, appCredentials);

    PhotobucketClient pbClient =
        new PhotobucketClient(
            jobId,
            monitor,
            photobucketCredentialsFactory.createCredential(tokensAndUrlAuthData),
            httpClient,
            jobStore,
            objectMapper,
            REQUESTER);
    // get root id and create top level album
    Assert.assertEquals(
        TOP_ALBUM_PB_ID, pbClient.createTopLevelAlbum(MAIN_PHOTO_ALBUM_TITLE_SUFFIX));
    // create nested album under top level
    Assert.assertEquals(NESTED_ALBUM_PB_ID, pbClient.createAlbum(nestedAlbum, ALBUM_TITLE_PREFIX));
    // verify user stats, upload by url, update metadata
    Assert.assertEquals(
        new ProcessingResult(IMAGE_PB_ID).extractOrThrow(),
        pbClient.uploadPhoto(photoModel).extractOrThrow());
  }
}
