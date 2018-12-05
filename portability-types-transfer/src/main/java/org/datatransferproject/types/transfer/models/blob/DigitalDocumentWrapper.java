package org.datatransferproject.types.transfer.models.blob;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.datatransferproject.types.common.models.DataModel;

/**
 * This is a wrapper around a {@link DtpDigitalDocument} that encodes a blobby file.
 * Data in the {@link DtpDigitalDocument} has a 1:1 relationship with the Schema.org spec, fields
 * this class represent DTP specific data that doesn't fit into the schema.org representation.
 */
public class DigitalDocumentWrapper extends DataModel {
  private final DtpDigitalDocument dtpDigitalDocument;
  private final String originalEncodingFormat;
  // This isn't in the schema.org spec and is only needed to store the bytes DTP will transfer
  private final String cachedContentId;

  @JsonCreator
  public DigitalDocumentWrapper(
      @JsonProperty("DtpDigitalDocument") DtpDigitalDocument dtpDigitalDocument,
      @JsonProperty("originalEncodingFormat") String originalEncodingFormat,
      @JsonProperty("cachedContentId") String cachedContentId
  ) {
    this.dtpDigitalDocument = dtpDigitalDocument;
    this.originalEncodingFormat = originalEncodingFormat;
    this.cachedContentId = cachedContentId;
  }

  public DtpDigitalDocument getDtpDigitalDocument() {
    return dtpDigitalDocument;
  }

  public String getCachedContentId() {
    return cachedContentId;
  }

  public String getOriginalEncodingFormat() {
    return originalEncodingFormat;
  }
}
