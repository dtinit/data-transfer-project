package org.dataportabilityproject.dataModels.mail;

public final class MailMessageModel {
  private final String rawString;

  public MailMessageModel(String rawString) {
    this.rawString = rawString;
  }

  /** RFC 2822 formatted and base64url encoded string **/
  public String getRawString() {
    return rawString;
  }
}
