package org.dataportabilityproject.dataModels.mail;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import java.util.List;

public final class MailMessageModel {
  private final String rawString;
  private final List<String> containerIds;

  public MailMessageModel(String rawString, List<String> containerIds) {
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
