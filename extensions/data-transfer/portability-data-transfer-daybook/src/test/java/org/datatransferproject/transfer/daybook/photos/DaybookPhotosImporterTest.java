/*
 * Copyright 2022 The Data-Portability Project Authors.
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

package org.datatransferproject.transfer.daybook.photos;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Collections;
import java.util.UUID;
import okhttp3.Call;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.spi.cloud.storage.TemporaryPerJobDataStore;
import org.datatransferproject.spi.transfer.idempotentexecutor.IdempotentImportExecutor;
import org.datatransferproject.spi.transfer.idempotentexecutor.InMemoryIdempotentImportExecutor;
import org.datatransferproject.types.common.models.photos.PhotoModel;
import org.datatransferproject.types.common.models.photos.PhotosContainerResource;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;

public class DaybookPhotosImporterTest {

  private Monitor monitor;
  private TemporaryPerJobDataStore jobStore;
  private IdempotentImportExecutor executor;
  private TokensAndUrlAuthData authData;
  private PhotosContainerResource data;
  private OkHttpClient client;

  @TempDir
  public Path folder;

  @BeforeEach
  public void setUp() {
    monitor = mock(Monitor.class);
    jobStore = mock(TemporaryPerJobDataStore.class);
    executor = new InMemoryIdempotentImportExecutor(monitor);
    authData = new TokensAndUrlAuthData("access-token", "refresh-token", "http://example.com");
    client = mock(OkHttpClient.class);
  }

  @Test
  public void testImportSinglePhoto() throws Exception {
    byte[] expectedImageData = {0xF, 0xA, 0xC, 0xE, 0xB, 0x0, 0x0, 0xC};
    InputStream inputStream = new ByteArrayInputStream(expectedImageData);
    TemporaryPerJobDataStore.InputStreamWrapper inputStreamWrapper =
        new TemporaryPerJobDataStore.InputStreamWrapper(inputStream);
    File file = folder.toFile();
    when(jobStore.getTempFileFromInputStream(any(), any(), any())).thenReturn(file);
    when(jobStore.getStream(any(), any())).thenReturn(inputStreamWrapper);

    PhotoModel photoModel =
        new PhotoModel("TestingPhotoTitle", "", "description", "", "TestingPhoto", "", true);
    PhotosContainerResource resource =
        new PhotosContainerResource(Collections.emptyList(), Collections.singletonList(photoModel));

    Call call = mock(Call.class);
    Response dummySuccessfulResponse =
        new Response.Builder()
            .code(200)
            .request(new Request.Builder().url("http://example.com").build())
            .protocol(Protocol.HTTP_1_1)
            .message("all good!")
            .body(ResponseBody.create(MediaType.parse("text/xml"), "<a>ok!</a>"))
            .build();
    when(call.execute()).thenReturn(dummySuccessfulResponse);
    when(client.newCall(any())).thenReturn(call);

    DaybookPhotosImporter daybookPhotosImporter =
        new DaybookPhotosImporter(monitor, client, jobStore, "http://daybook.com", "exporter");
    daybookPhotosImporter.importItem(UUID.randomUUID(), executor, authData, resource);
    ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
    verify(client, times(1)).newCall(requestCaptor.capture());

    RequestBody untypedBody = requestCaptor.getValue().body();
    assertTrue(untypedBody instanceof FormBody);
    FormBody actual = (FormBody) untypedBody;
    assertEquals("image", actual.name(0));

    String base64Image = actual.value(0);
    byte[] actualImageData = Base64.getDecoder().decode(base64Image);
    assertArrayEquals(expectedImageData, actualImageData);
  }
}
