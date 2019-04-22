/*
 * Copyright 2019 The Data Transfer Project Authors.
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

package org.datatransferproject.types.transfer.serviceconfig;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Preconditions;

/**
 * POJO Specification for Transfer Service specific configuration details.
 */
public class TransferServiceConfigSpecification {

  @JsonProperty("perUserRateLimit")
  private final double perUserRateLimit;

  public TransferServiceConfigSpecification(
      @JsonProperty("perUserRateLimit") double perUserRateLimit) {
    Preconditions.checkArgument(
        perUserRateLimit > 0,
        "perUserRateLimit must be greater than zero");
    this.perUserRateLimit = perUserRateLimit;
  }

  /** The number of operations per second allowed for a user. **/
  public double getPerUserRateLimit() {
    return perUserRateLimit;
  }
}
