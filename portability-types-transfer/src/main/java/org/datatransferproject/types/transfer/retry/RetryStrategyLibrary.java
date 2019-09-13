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
import com.google.common.base.Preconditions;
import java.util.List;

/**
 * Class used by {@link RetryingCallable} to determine which {@link RetryStrategy} to use given a
 * particular error.
 *
 * Internally, compares the input {@link Throwable} to every {@link RetryMapping} in its ordered
 * List until it finds the appropriate RetryStrategy.  The list of mappings should be ordered such
 * that specific cases come first, followed by general cases.
 *
 * If the Throwable does not match any RetryStrategy, then a default RetryStrategy is returned.
 *
 * NOTE: Our core library only supports reading RetryStrategyLibraries from JSON or YAML format.
 * You are welcome to write your own parser for any other config languages you like, as long as it
 * can be ultimately parsed by Jackson.
 */
public class RetryStrategyLibrary {

  @JsonProperty("strategyMappings")
  private final List<RetryMapping> retryMappings;
  @JsonProperty("defaultRetryStrategy")
  private final RetryStrategy defaultRetryStrategy;

  public RetryStrategyLibrary(@JsonProperty("strategyMappings") List<RetryMapping> retryMappings,
      @JsonProperty("defaultRetryStrategy") RetryStrategy defaultRetryStrategy) {
    Preconditions.checkArgument(defaultRetryStrategy != null, "Default retry strategy cannot be null");
    this.retryMappings = retryMappings;
    this.defaultRetryStrategy = defaultRetryStrategy;
  }

  /**
   * Returns the best {@link RetryStrategy} for a given Throwable.  If there are no matches, returns
   * the default RetryStrategy.
   *
   * Right now it just looks at the message in the Throwable and tries to find a matching regex in
   * its internal library.  Later on it will use more and more of the Throwable to make a decision.
   */
  public RetryStrategy checkoutRetryStrategy(Throwable throwable) {
    // TODO: determine retry strategy based on full information in Throwable
    for (RetryMapping mapping : retryMappings) {
      if (mapping.matchesThrowable(throwable)) {
        return mapping.getStrategy();
      }
    }
    return defaultRetryStrategy;
  }

  public RetryStrategy getDefaultRetryStrategy() {
    return defaultRetryStrategy;
  }

  @Override
  public String toString() {
    return "RetryStrategyLibrary{" +
        "retryMappings=" + retryMappings +
        ", defaultRetryStrategy=" + defaultRetryStrategy +
        '}';
  }
}
