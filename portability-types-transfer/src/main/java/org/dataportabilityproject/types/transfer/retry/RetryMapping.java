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

package org.dataportabilityproject.types.transfer.retry;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RetryMapping {

  @JsonProperty("regexes")
  private String[] regexes;
  @JsonProperty("strategy")
  private RetryStrategy strategy;

  public RetryMapping(@JsonProperty("regexes") String[] regexes,
      @JsonProperty("strategy") RetryStrategy strategy) {
    this.regexes = regexes;
    this.strategy = strategy;
  }

  public String[] getRegexes() {
    return regexes;
  }

  public RetryStrategy getStrategy() {
    return strategy;
  }
}
