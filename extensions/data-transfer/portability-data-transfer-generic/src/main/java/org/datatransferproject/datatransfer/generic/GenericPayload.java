package org.datatransferproject.datatransfer.generic;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
public class GenericPayload<T> {
  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY)
  private final T payload;

  private final String schemaSource;
  // TODO: update
  private final String apiVersion = "0.1.0";

  @JsonCreator
  public GenericPayload(@JsonProperty T payload, @JsonProperty String schemaSource) {
    this.payload = payload;
    this.schemaSource = schemaSource;
  }

  public T getPayload() {
    return payload;
  }

  public String getApiVersion() {
    return apiVersion;
  }

  public String getSchemaSource() {
    return schemaSource;
  }
}
