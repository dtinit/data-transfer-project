/*
 * Copyright 2021 The Data-Portability Project Authors.
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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import okhttp3.*;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.spi.transfer.idempotentexecutor.IdempotentImportExecutor;
import org.datatransferproject.spi.transfer.provider.ImportResult;
import org.datatransferproject.spi.transfer.provider.Importer;
import org.datatransferproject.transfer.JobMetadata;
import org.datatransferproject.types.common.models.social.SocialActivityAttachment;
import org.datatransferproject.types.common.models.social.SocialActivityAttachmentType;
import org.datatransferproject.types.common.models.social.SocialActivityContainerResource;
import org.datatransferproject.types.common.models.social.SocialActivityModel;
import org.datatransferproject.types.common.models.social.SocialActivityType;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;

/** Imports posts to Daybook */
public class DaybookPostsImporter
    implements Importer<TokensAndUrlAuthData, SocialActivityContainerResource> {

  private final Monitor monitor;
  private final ObjectMapper objectMapper;
  private final OkHttpClient client;
  private final String baseUrl;

  public DaybookPostsImporter(
      Monitor monitor, OkHttpClient client, ObjectMapper objectMapper, String baseUrl) {
    this.baseUrl = baseUrl;
    this.client = client;
    this.monitor = monitor;
    this.objectMapper = objectMapper;
  }

  @Override
  public ImportResult importItem(
      UUID jobId,
      IdempotentImportExecutor executor,
      TokensAndUrlAuthData authData,
      SocialActivityContainerResource resource)
      throws Exception {
    if (resource == null) {
      // Nothing to import
      return ImportResult.OK;
    }

    monitor.debug(
        () -> String.format("Number of Posts: %d", resource.getCounts().get("activitiesCount")));

    // Import social activity
    for (SocialActivityModel activity : resource.getActivities()) {
      if (activity.getType() == SocialActivityType.NOTE
          || activity.getType() == SocialActivityType.POST) {
        executor.executeAndSwallowIOExceptions(
            Integer.toString(activity.hashCode()),
            activity.getTitle(),
            () -> insertActivity(executor, activity, authData));
      }
    }

    return new ImportResult(ImportResult.ResultType.OK);
  }

  private String insertActivity(
      IdempotentImportExecutor executor,
      SocialActivityModel activity,
      TokensAndUrlAuthData authData)
      throws IOException {
    Map<String, String> imageMap = new HashMap<>();
    Map<String, String> linkMap = new HashMap<>();

    String content = activity.getContent() == null ? "" : activity.getContent();
    String title = activity.getTitle() == null ? "" : activity.getTitle();
    String location =
        activity.getLocation() == null || activity.getLocation().getName() == null
            ? ""
            : activity.getLocation().getName();
    String published =
        activity.getPublished().toString() == null ? "" : activity.getPublished().toString();

    Request.Builder requestBuilder = new Request.Builder().url(baseUrl);
    requestBuilder.header("token", authData.getAccessToken());

    FormBody.Builder builder = new FormBody.Builder().add("type", "POSTS");
    builder.add("exporter", JobMetadata.getExportService());
    builder.add("content", content);
    builder.add("title", title);
    builder.add("location", location);
    builder.add("published", published);

    Collection<SocialActivityAttachment> linkAttachments =
        activity.getAttachments().stream()
            .filter(attachment -> attachment.getType() == SocialActivityAttachmentType.LINK)
            .collect(Collectors.toList());
    Collection<SocialActivityAttachment> imageAttachments =
        activity.getAttachments().stream()
            .filter(attachment -> attachment.getType() == SocialActivityAttachmentType.IMAGE)
            .collect(Collectors.toList());

    // Just put link attachments at the bottom of the post, as we
    // don't know how they were laid out in the originating service.
    if (!linkAttachments.isEmpty()) {

      for (SocialActivityAttachment attachment : linkAttachments) {
        linkMap.put(attachment.getName(), attachment.getUrl());
      }
      try {
        String json = objectMapper.writeValueAsString(linkMap);
        builder.add("link", json);
      } catch (JsonProcessingException e) {
        monitor.info(() -> String.format("Error processing JSON: %s", e.getMessage()));
      }
    }

    if (!imageAttachments.isEmpty()) {

      for (SocialActivityAttachment image : imageAttachments) {
        imageMap.put(image.getName() != null ? image.getName() : image.getUrl(), image.getUrl());
      }
      try {
        String json = objectMapper.writeValueAsString(imageMap);
        builder.add("image", json);
      } catch (JsonProcessingException e) {
        monitor.info(() -> String.format("Error processing JSON: %s", e.getMessage()));
      }
    }

    FormBody formBody = builder.build();
    requestBuilder.post(formBody);

    try (Response response = client.newCall(requestBuilder.build()).execute()) {
      int code = response.code();
      // Though sometimes it returns error code for success requests
      if (code < 200 || code > 299) {
        throw new IOException(
            String.format(
                "Error occurred in request for adding entry, message: %s", response.message()));
      }

      return response.message();
    }
  }
}
