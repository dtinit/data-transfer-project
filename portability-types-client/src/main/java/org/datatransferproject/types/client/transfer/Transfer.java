/*
 * Copyright 2018 The Data Transfer Project Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.datatransferproject.types.client.transfer;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/** A data transfer operation. */
public class Transfer {
  public enum State {
    CREATED,
    SUBMITTED,
    IN_PROGRESS,
    COMPLETE
  }

  private final String id;
  private final String link;
  private final String source;
  private final String destination;
  private final String transferDataType;
  private final State state;

  @JsonCreator
  public Transfer(
      @JsonProperty(value = "id", required = true) String id,
      @JsonProperty(value = "state", required = true) State state,
      @JsonProperty(value = "link", required = false) String link,
      @JsonProperty(value = "source", required = true) String source,
      @JsonProperty(value = "destination", required = true) String destination,
      @JsonProperty(value = "transferDataType", required = true) String transferDataType) {
    this.id = id;
    this.state = state;
    this.link = link;

    this.source = source;
    this.destination = destination;
    this.transferDataType = transferDataType;
  }

  public String getId() {
    return id;
  }

  public String getLink() {
    return link;
  }

  public String getSource() {
    return source;
  }

  public String getDestination() {
    return destination;
  }

  public String getTransferDataType() {
    return transferDataType;
  }

  public State getState() {
    return state;
  }
}
