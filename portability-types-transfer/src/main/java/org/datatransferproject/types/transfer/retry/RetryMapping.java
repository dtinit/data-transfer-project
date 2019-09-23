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

package org.datatransferproject.types.transfer.retry;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Arrays;

/**
 * Class that determines whether a given {@link Throwable} is a match for its {@link RetryStrategy}.
 * At the moment, this class only examines the string of the Throwable and determines whether it
 * matches any of its string regular expressions.
 *
 * NOTE: Our core library only supports reading RetryMappings from JSON or YAML format.
 */
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

  public boolean matchesThrowable(Throwable throwable) {
    // TODO: examine entire throwable, not just toString
    String input = throwable.toString();
    for (String regex : regexes) {
      if (input.matches(regex)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public String toString() {
    return "RetryMapping{" +
        "regexes=" + Arrays.toString(regexes) +
        ", strategy=" + strategy +
        '}';
  }
}
