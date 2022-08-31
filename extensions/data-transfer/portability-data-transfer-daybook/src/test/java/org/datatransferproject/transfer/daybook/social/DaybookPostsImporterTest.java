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

package org.datatransferproject.transfer.daybook.social;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import java.time.Instant;
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
import org.datatransferproject.spi.transfer.idempotentexecutor.IdempotentImportExecutor;
import org.datatransferproject.spi.transfer.idempotentexecutor.InMemoryIdempotentImportExecutor;
import org.datatransferproject.types.common.models.social.SocialActivityActor;
import org.datatransferproject.types.common.models.social.SocialActivityContainerResource;
import org.datatransferproject.types.common.models.social.SocialActivityLocation;
import org.datatransferproject.types.common.models.social.SocialActivityModel;
import org.datatransferproject.types.common.models.social.SocialActivityType;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;
import org.junit.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;

public class DaybookPostsImporterTest {

  private Monitor monitor;
  private IdempotentImportExecutor executor;
  private TokensAndUrlAuthData authData;
  private OkHttpClient client;

  @TempDir
  public Path folder;

  @BeforeEach
  public void setUp() {
    monitor = mock(Monitor.class);
    executor = new InMemoryIdempotentImportExecutor(monitor);
    authData = new TokensAndUrlAuthData("access-token", "refresh-token", "http://example.com");
    client = mock(OkHttpClient.class);
  }

  @Test
  public void testImportSingleActivity() throws Exception {
    String postContent = "activityContent";
    SocialActivityModel activity =
        new SocialActivityModel(
            "activityId",
            Instant.now(),
            SocialActivityType.POST,
            Collections.emptyList(),
            new SocialActivityLocation("test", 1.1, 2.2),
            "activityTitle",
            postContent,
            "activityUrl");
    SocialActivityContainerResource resource =
        new SocialActivityContainerResource(
            "123",
            new SocialActivityActor("321", "John Doe", "url"),
            Collections.singletonList(activity));

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

    DaybookPostsImporter importer =
        new DaybookPostsImporter(
            monitor, client, new ObjectMapper(), "http://example.com", "export-service");
    importer.importItem(UUID.randomUUID(), executor, authData, resource);
    ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
    verify(client, times(1)).newCall(requestCaptor.capture());

    RequestBody untypedBody = requestCaptor.getValue().body();
    assertTrue(untypedBody instanceof FormBody);
    FormBody actual = (FormBody) untypedBody;
    assertEquals(
        "DaybookPostsImporter changed the order of fields in the body.", "content", actual.name(2));
    assertEquals(postContent, actual.value(2));
  }
}
