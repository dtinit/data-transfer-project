/*
 * Copyright 2020 The Data Transfer Project Authors.
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

package org.datatransferproject.transfer.instagram.videos;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.datatransferproject.spi.transfer.provider.ExportResult;
import org.datatransferproject.transfer.instagram.common.InstagramApiClient;
import org.datatransferproject.transfer.instagram.model.Child;
import org.datatransferproject.transfer.instagram.model.MediaFeedData;
import org.datatransferproject.transfer.instagram.model.MediaResponse;
import org.datatransferproject.types.common.ExportInformation;
import org.datatransferproject.types.common.StringPaginationToken;
import org.datatransferproject.types.common.models.videos.VideoAlbum;
import org.datatransferproject.types.common.models.videos.VideoObject;
import org.datatransferproject.types.common.models.videos.VideosContainerResource;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class InstagramVideoExporterTest {
  private InstagramVideoExporter exporter;
  private UUID uuid = UUID.randomUUID();
  private MediaResponse mediaResponse;

  @Before
  public void setUp() throws IOException {
    InstagramApiClient instagramApiClient = mock(InstagramApiClient.class);

    String mediaResponseJson =
        "{\n"
            + "  \"data\": [\n"
            + "    {\n"
            + "      \"id\": \"17868009584014245\",\n"
            + "      \"media_url\": \"https://test\",\n"
            + "      \"media_type\": \"CAROUSEL_ALBUM\",\n"
            + "      \"caption\": \"Test carousel\",\n"
            + "      \"timestamp\": \"2020-09-21T00:00:00+0000\",\n"
            + "      \"children\": {\n"
            + "        \"data\": [\n"
            + "          {\n"
            + "            \"id\": \"1\",\n"
            + "            \"media_url\": \"https://testimage1\",\n"
            + "            \"media_type\": \"IMAGE\"\n"
            + "          },\n"
            + "          {\n"
            + "            \"id\": \"17879726539831812\",\n"
            + "            \"media_url\": \"https://test5\",\n"
            + "            \"media_type\": \"VIDEO\"\n"
            + "          }\n"
            + "        ]\n"
            + "      }\n"
            + "    },\n"
            + "    {\n"
            + "      \"id\": \"17879582080807068\",\n"
            + "      \"media_url\": \"https://test6\",\n"
            + "      \"media_type\": \"VIDEO\",\n"
            + "      \"caption\": \"test indie\",\n"
            + "      \"timestamp\": \"2020-09-11T16:26:32+0000\"\n"
            + "    },\n"
            + "    {\n"
            + "      \"id\": \"4\",\n"
            + "      \"media_url\": \"https://testimage4\",\n"
            + "      \"media_type\": \"IMAGE\",\n"
            + "      \"caption\": \"test\",\n"
            + "      \"timestamp\": \"2020-09-11T16:25:54+0000\"\n"
            + "    }\n"
            + "  ],\n"
            + "  \"paging\": {\n"
            + "    \"cursors\": {\n"
            + "      \"after\": \"QVFIUkVUZAU5mY2ROOGhJVjhsVF96d1JFWUNSQmJGY1F5VHFBZAmJEWVotdE9sU09aZAGM5MVF2b3pJNVg5Y21EQXN5ZAkZAXdkVYNEMzQmFTb0NTM0M2ZAXRJR1hn\"\n"
            + "    }\n"
            + "  }\n"
            + "}";

    ObjectMapper mapper = new ObjectMapper();
    mediaResponse = mapper.readValue(mediaResponseJson, MediaResponse.class);
    when(instagramApiClient.makeRequest(Mockito.any())).thenReturn(mediaResponse);

    exporter = new InstagramVideoExporter(null, null, null, instagramApiClient);
  }

  @Test
  public void testExportAlbum() throws IOException {
    ExportResult<VideosContainerResource> result =
        exporter.export(
            uuid, new TokensAndUrlAuthData("accessToken", null, null), Optional.empty());

    assertEquals(ExportResult.ResultType.CONTINUE, result.getType());
    VideosContainerResource exportedData = result.getExportedData();
    assertEquals(1, exportedData.getAlbums().size());
    assertEquals(
        new VideoAlbum(
            InstagramVideoExporter.DEFAULT_ALBUM_ID,
            InstagramVideoExporter.DEFAULT_ALBUM_NAME,
            InstagramVideoExporter.DEFAULT_ALBUM_DESCRIPTION),
        exportedData.getAlbums().toArray()[0]);
    StringPaginationToken paginationToken =
        (StringPaginationToken) result.getContinuationData().getPaginationData();
    assertEquals(paginationToken.getToken(), InstagramApiClient.getMediaBaseUrl());
  }

  @Test
  public void testExportVideos() throws IOException {
    ExportResult<VideosContainerResource> result =
        exporter.export(
            uuid,
            new TokensAndUrlAuthData("accessToken", null, null),
            Optional.of(
                new ExportInformation(
                    new StringPaginationToken(InstagramApiClient.getMediaBaseUrl()), null)));

    assertEquals(ExportResult.ResultType.END, result.getType());
    VideosContainerResource exportedData = result.getExportedData();
    assertEquals(0, exportedData.getAlbums().size());
    assertEquals(2, exportedData.getVideos().size());

    // check carousel video
    MediaFeedData carouselAlbum = mediaResponse.getData().get(0);
    Child carouselVideo = carouselAlbum.getChildren().getData().get(1);
    assertEquals(
        new VideoObject(
            carouselVideo.getId() + ".mp4",
            carouselVideo.getMediaUrl(),
            carouselAlbum.getCaption(),
            "video/mp4",
            carouselVideo.getId(),
            InstagramVideoExporter.DEFAULT_ALBUM_ID,
            false),
        exportedData.getVideos().toArray()[0]);

    // check standalone video
    MediaFeedData standaloneVideo = mediaResponse.getData().get(1);
    assertEquals(
        new VideoObject(
            standaloneVideo.getId() + ".mp4",
            standaloneVideo.getMediaUrl(),
            standaloneVideo.getCaption(),
            "video/mp4",
            standaloneVideo.getId(),
            InstagramVideoExporter.DEFAULT_ALBUM_ID,
            false),
        exportedData.getVideos().toArray()[1]);
  }
}
