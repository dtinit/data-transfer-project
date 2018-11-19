package org.datatransferproject.types.transfer.models.blob;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.datatransferproject.types.transfer.models.DataModel;

/**
 * This is intended to by a sub set of schema.org's DigitalDocument
 * (see: https://schema.org/DigitalDocument).  Additional fields from the schema.org spec should
 * be added as needed.
 */
public class DigitalDocument extends DataModel {
  private final String name;
  // Rfc3339 encoded string of the last modified date
  private final String dateModified;
  private final String encodingFormat;
  // This isn't in the schema.org spec but represents the original/proprietary format to support
  // better round tripping
  private final String originalEncodingFormat;
  // This isn't in the schema.org spec and is only needed to store the bytes DTP will transfer
  private final String cachedContentId;

  @JsonCreator
  public DigitalDocument(
      @JsonProperty("name") String name,
      @JsonProperty("dateModified") String dateModified,
      @JsonProperty("encodingFormat") String encodingFormat,
      @JsonProperty("originalEncodingFormat") String originalEncodingFormat,
      @JsonProperty("cachedContentId") String cachedContentId
  ) {
    this.name = name;
    this.dateModified = dateModified;
    this.encodingFormat = encodingFormat;
    this.originalEncodingFormat = originalEncodingFormat;
    this.cachedContentId = cachedContentId;
  }

  public String getName() {
    return name;
  }

  public String getCachedContentId() {
    return cachedContentId;
  }

  public String getDateModified() {
    return dateModified;
  }

  public String getEncodingFormat() {
    return encodingFormat;
  }

  public String getOriginalEncodingFormat() {
    return originalEncodingFormat;
  }
}
