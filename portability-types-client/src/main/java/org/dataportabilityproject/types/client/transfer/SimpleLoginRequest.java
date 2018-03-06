/*
 * Copyright 2018 The Data-Portability Project Authors.
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

package org.dataportabilityproject.types.client.transfer;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class SimpleLoginRequest {
  // TODO: Determine how to avoid storing the password in clear-text.
  private final String username;
  private final String password;

  @JsonCreator
  public SimpleLoginRequest(
      @JsonProperty(value = "username", required = true) String username,
      @JsonProperty(value = "password", required = true) String password) {
    this.username = username;
    this.password = password;
  }

  @ApiModelProperty
  public String getPassword() {
    return password;
  }

  @ApiModelProperty
  public String getUsername() {
    return username;
  }
}
