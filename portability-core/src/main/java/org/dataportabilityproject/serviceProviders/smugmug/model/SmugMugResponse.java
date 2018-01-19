/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
