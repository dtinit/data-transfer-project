package org.dataportabilityproject.dataModels.mail;

import org.dataportabilityproject.dataModels.DataModel;
import org.dataportabilityproject.shared.PortableDataType;

public final class MailMessageModel implements DataModel {
  private final String rawString;

  public MailMessageModel(String rawString) {
    this.rawString = rawString;
  }

  /** RFC 2822 formatted and base64url encoded string **/
  public String getRawString() {
    return rawString;
  }

  @Override
  public PortableDataType getDataType() {
    return PortableDataType.MAIL;
  }
}
