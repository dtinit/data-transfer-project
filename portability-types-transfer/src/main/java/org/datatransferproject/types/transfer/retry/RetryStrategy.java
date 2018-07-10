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

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

/**
 * Defines a retry strategy - i.e., how many retry attempts should be made, and at what intervals.
 *
 * NOTE: Our core library only supports reading RetryStrategies from JSON or YAML format.
 */
@JsonTypeInfo(use = Id.NAME,
    include = As.PROPERTY,
    property = "type",
    visible = true)
@JsonSubTypes({
    @JsonSubTypes.Type(value = UniformRetryStrategy.class, name = "Uniform"),
    @JsonSubTypes.Type(value = ExponentialBackoffStrategy.class, name = "Exponential"),
    @JsonSubTypes.Type(value = NoRetryStrategy.class, name = "Fatal")
})
public interface RetryStrategy {

  /**
   * Shows whether another retry is possible or not, given the number of tries so far
   */
  boolean canTryAgain(int tries);

  /**
   * Amount of time (in milliseconds) until next retry.  Should return a negative number if no more
   * retries are left.
   */
  long getNextIntervalMillis(int tries);

  /**
   * Gets milliseconds until the next retry, given elapsed time so far
   */
  long getRemainingIntervalMillis(int tries, long elapsedMillis);
}
