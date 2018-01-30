/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.dataportabilityproject.types.transfer.models.mail;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import java.util.Collection;
import org.dataportabilityproject.types.transfer.models.ContainerResource;

/**
 * A Wrapper for all the possible objects that can be returned by a mail exporter.
 */
@JsonTypeName("MailContainerResource")
public class MailContainerResource extends ContainerResource {
  private final Collection<MailContainerModel> folders;
  private final Collection<MailMessageModel> messages;

  @JsonCreator
  public MailContainerResource(
      @JsonProperty("folders") Collection<MailContainerModel> folders,
      @JsonProperty("messages") Collection<MailMessageModel> messages) {
    this.messages = (messages == null) ? ImmutableList.of() : messages;
    this.folders = (folders == null) ? ImmutableList.of() : folders;
  }

  public Collection<MailMessageModel> getMessages() {
    return messages;
  }

  public Collection<MailContainerModel> getFolders() {
    return folders;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("folders", folders.size())
        .add("messages", messages.size())
        .toString();
  }
}
