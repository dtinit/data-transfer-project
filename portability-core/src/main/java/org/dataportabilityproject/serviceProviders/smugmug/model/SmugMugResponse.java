package org.dataportabilityproject.serviceProviders.smugmug.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A generic wrapper around SmugMug html responses.
 */
public class SmugMugResponse<T> {
  @JsonProperty("Response")
  private T response;

  //@JsonProperty("Response")
  //private HashMap<String, Object> fakeObject;

  @JsonProperty("Code")
  private int code;

  @JsonProperty("Message")
  private String message;

  public int getCode() {
    return code;
  }

  public String getMessage() {
    return message;
  }

  public T getResponse() {
    return response;
  }
}
