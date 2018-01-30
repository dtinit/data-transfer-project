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
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import java.util.List;

public final class MailMessageModel {
  private final String rawString;
  private final List<String> containerIds;

  @JsonCreator
  public MailMessageModel(
      @JsonProperty("rawString") String rawString,
      @JsonProperty("containerIds") List<String> containerIds) {
    this.rawString = rawString;
    this.containerIds = (containerIds == null) ? ImmutableList.of() : containerIds;
  }

  /** RFC 2822 formatted and base64url encoded string **/
  public String getRawString() {
    return rawString;
  }
  /** Container, e.g. folder or label, this message belongs to **/
  public List<String> getContainerIds() { return containerIds; }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("rawString", rawString.length())
        .add("containerIds", containerIds.size())
        .toString();
  }
}
