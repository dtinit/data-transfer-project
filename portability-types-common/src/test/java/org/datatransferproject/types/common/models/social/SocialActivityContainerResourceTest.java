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

package org.datatransferproject.types.common.models.social;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.collect.ImmutableList;
import com.google.common.truth.Truth;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;
import org.datatransferproject.types.common.models.ContainerResource;
import org.junit.Test;

public class SocialActivityContainerResourceTest {

  @Test
  public void verifySerializeDeserialize() throws Exception {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    objectMapper.registerSubtypes(SocialActivityContainerResource.class);

    Instant timePublished = OffsetDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC).toInstant();

    SocialActivityLocation location = new SocialActivityLocation("Somewhere", 0, 0);
    SocialActivityAttachment linkAttachment =
        new SocialActivityAttachment(
            SocialActivityAttachmentType.LINK, "some url", "some link", "this is the link text");
    SocialActivityAttachment imageAttachment =
        new SocialActivityAttachment(
            SocialActivityAttachmentType.IMAGE,
            "some image url",
            "some image",
            "this is the image description");
    SocialActivityAttachment videoAttachment =
        new SocialActivityAttachment(
            SocialActivityAttachmentType.VIDEO, "some url", "some video", "this is the video text");
    List<SocialActivityModel> activities =
        ImmutableList.of(
            new SocialActivityModel(
                "id1",
                timePublished,
                SocialActivityType.NOTE,
                null,
                null,
                "Write Better tests",
                "Here's some sample content",
                "https://facebook.com"),
            new SocialActivityModel(
                "id2",
                timePublished,
                SocialActivityType.POST,
                ImmutableList.of(linkAttachment, imageAttachment, videoAttachment),
                null,
                "Write Some more tests",
                "Here's some sample post content",
                "https://facebook.com/dtp"),
            new SocialActivityModel(
                "id3",
                timePublished,
                SocialActivityType.CHECKIN,
                null,
                location,
                "Write Some location tests",
                "Here's some sample checkin content",
                "https://facebook.com/dtp"));

    SocialActivityActor actor =
        new SocialActivityActor("actor ID", "actor url", "facebook.com/zuck");

    ContainerResource data = new SocialActivityContainerResource("someID", actor, activities);

    String serialized = objectMapper.writeValueAsString(data);

    ContainerResource deserializedModel =
        objectMapper.readValue(serialized, ContainerResource.class);

    Truth.assertThat(deserializedModel).isNotNull();
    Truth.assertThat(deserializedModel).isInstanceOf(SocialActivityContainerResource.class);
    SocialActivityContainerResource deserialized =
        (SocialActivityContainerResource) deserializedModel;
    Truth.assertThat(deserialized.getActivities()).hasSize(3);
    SocialActivityModel postModel =
        deserialized.getActivities().stream()
            .filter(socialActivityModel -> socialActivityModel.getType() == SocialActivityType.POST)
            .collect(Collectors.toList())
            .get(0);
    List<SocialActivityAttachment> imageActivityAttachment =
        postModel.getAttachments().stream()
            .filter(
                socialActivityAttachment ->
                    socialActivityAttachment.getType() == SocialActivityAttachmentType.IMAGE)
            .collect(Collectors.toList());
    List<SocialActivityAttachment> linkActivityAttachment =
        postModel.getAttachments().stream()
            .filter(
                socialActivityAttachment ->
                    socialActivityAttachment.getType() == SocialActivityAttachmentType.LINK)
            .collect(Collectors.toList());
    List<SocialActivityAttachment> videoActivityAttachment =
        postModel.getAttachments().stream()
            .filter(
                socialActivityAttachment ->
                    socialActivityAttachment.getType() == SocialActivityAttachmentType.VIDEO)
            .collect(Collectors.toList());
    Truth.assertThat(imageActivityAttachment).hasSize(1);
    Truth.assertThat(linkActivityAttachment).hasSize(1);
    Truth.assertThat(videoActivityAttachment).hasSize(1);
    Truth.assertThat(deserialized).isEqualTo(data);
    Truth.assertThat(
            deserialized
                .getCounts()
                .get(SocialActivityContainerResource.ACTIVITIES_COUNT_DATA_NAME))
        .isEqualTo(activities.size());
  }
}
