package org.dataportabilityproject.dataModels.mail;

import com.google.common.collect.ImmutableList;
import java.util.Collection;
import org.dataportabilityproject.dataModels.ContinuationInformation;
import org.dataportabilityproject.dataModels.DataModel;

/**
 * A Wrapper for all the possible objects that can be returned by a mail exporter.
 */
public class MailModelWrapper implements DataModel {
  private final Collection<MailMessageModel> messages;
  private final ContinuationInformation continuationInformation;

  public MailModelWrapper(Collection<MailMessageModel> messages,
      ContinuationInformation continuationInformation) {
    this.messages = messages == null ? ImmutableList.of() : messages;
    this.continuationInformation = continuationInformation;
  }

  @Override
  public ContinuationInformation getContinuationInformation() {
    return continuationInformation;
  }

  public Collection<MailMessageModel> getMessages() {
    return messages;
  }
}
