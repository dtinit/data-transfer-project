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
