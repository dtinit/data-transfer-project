package org.datatransferproject.datatransfer.generic;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Optional;
import javax.annotation.Nullable;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ErrorResponse {
  private final String error;
  private final Optional<String> errorDescription;
  
  @JsonCreator
  public ErrorResponse(
      @JsonProperty(value = "error", required = true) String error,
      @Nullable @JsonProperty("error_description") String errorDescription) {
    this.error = error;
    this.errorDescription = Optional.ofNullable(errorDescription);
  }
  
  public String getError() {
    return error;
  }
  
  public Optional<String> getErrorDescription() {
    return errorDescription;
  }
  
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append(error);
    if (errorDescription.isPresent()) {
      builder.append(" - ");
      builder.append(errorDescription.get());
    }
    return builder.toString();
  }
}
