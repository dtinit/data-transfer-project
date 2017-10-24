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
package org.dataportabilityproject.dataModels.mail;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.util.Collection;
import org.dataportabilityproject.dataModels.ContinuationInformation;
import org.dataportabilityproject.dataModels.DataModel;

/**
 * A Wrapper for all the possible objects that can be returned by a mail exporter.
 */
public class MailModelWrapper implements DataModel {
  private final Collection<MailContainerModel> folders;
  private final Collection<MailMessageModel> messages;
  private final ContinuationInformation continuationInformation;

  public MailModelWrapper(
      Collection<MailContainerModel> folders,
      Collection<MailMessageModel> messages,
      ContinuationInformation continuationInformation) {
    this.messages = (messages == null) ? ImmutableList.of() : messages;
    this.folders = (folders == null) ? ImmutableList.of() : folders;
    this.continuationInformation = continuationInformation;
  }

  public Collection<MailMessageModel> getMessages() {
    return messages;
  }

  @Override
  public ContinuationInformation getContinuationInformation() {
    return continuationInformation;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("folders", folders.size())
        .add("messages", messages.size())
        .add("continuationInformation", continuationInformation)
        .toString();
  }
}
