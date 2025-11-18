/*
 * Copyright 2022 The Data Transfer Project Authors.
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

package org.datatransferproject.types.common.models;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.datatransferproject.types.common.models.blob.BlobbyStorageContainerResource;
import org.datatransferproject.types.common.models.calendar.CalendarContainerResource;
import org.datatransferproject.types.common.models.mail.MailContainerResource;
import org.datatransferproject.types.common.models.media.MediaContainerResource;
import org.datatransferproject.types.common.models.music.MusicContainerResource;
import org.datatransferproject.types.common.models.photos.PhotosContainerResource;
import org.datatransferproject.types.common.models.playlists.PlaylistContainerResource;
import org.datatransferproject.types.common.models.social.SocialActivityContainerResource;
import org.datatransferproject.types.common.models.tasks.TaskContainerResource;
import org.datatransferproject.types.common.models.videos.VideosContainerResource;

/**
 * A resource that contains data items such as a photo album or song list.
 *
 * <p>Concrete subtypes must use {@link com.fasterxml.jackson.annotation.JsonTypeName} to specify a
 * type discriminator used for deserialization.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
@JsonSubTypes({
  @JsonSubTypes.Type(PhotosContainerResource.class),
  @JsonSubTypes.Type(VideosContainerResource.class),
  @JsonSubTypes.Type(MediaContainerResource.class),
  @JsonSubTypes.Type(MailContainerResource.class),
  @JsonSubTypes.Type(CalendarContainerResource.class),
  @JsonSubTypes.Type(TaskContainerResource.class),
  @JsonSubTypes.Type(PlaylistContainerResource.class),
  @JsonSubTypes.Type(SocialActivityContainerResource.class),
  @JsonSubTypes.Type(IdOnlyContainerResource.class),
  @JsonSubTypes.Type(DateRangeContainerResource.class),
  @JsonSubTypes.Type(MusicContainerResource.class),
  @JsonSubTypes.Type(BlobbyStorageContainerResource.class)
})
public abstract class ContainerResource extends DataModel {}
