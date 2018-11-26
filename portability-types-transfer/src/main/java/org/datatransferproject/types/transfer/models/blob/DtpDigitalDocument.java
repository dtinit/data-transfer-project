package org.datatransferproject.types.transfer.models.blob;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * This is intended to by a sub set of schema.org's DigitalDocumentWrapper
 * (see: ).  Additional fields from the schema.org spec should
 * be added as needed.
 */
public class DtpDigitalDocument {

  private final String name;
  // Rfc3339 encoded string of the last modified date
  private final String dateModified;
  private final String encodingFormat;

  @JsonCreator
  public DtpDigitalDocument(
      @JsonProperty("name") String name,
      @JsonProperty("dateModified") String dateModified,
      @JsonProperty("encodingFormat") String encodingFormat
  ) {
    this.name = name;
    this.dateModified = dateModified;
    this.encodingFormat = encodingFormat;
  }

  public String getName() {
    return name;
  }

  public String getDateModified() {
    return dateModified;
  }

  public String getEncodingFormat() {
    return encodingFormat;
  }
}
