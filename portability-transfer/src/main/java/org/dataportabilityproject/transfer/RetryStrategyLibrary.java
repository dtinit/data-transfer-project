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

package org.dataportabilityproject.transfer;

import com.google.gdata.util.common.base.Pair;
import java.util.List;

public class RetryStrategyLibrary {
  private final List<Pair<String, RetryStrategy>> strategyMapping;
  private final RetryStrategy defaultRetryStrategy;

  RetryStrategyLibrary(List<Pair<String, RetryStrategy>> strategyMapping,
      RetryStrategy defaultRetryStrategy) {
    this.strategyMapping = strategyMapping;
    this.defaultRetryStrategy = defaultRetryStrategy;
  }

  public RetryStrategy checkoutRetryStrategy(Throwable throwable) {
    String exceptionMessage = throwable.getMessage();
    for (Pair<String, RetryStrategy> entry : strategyMapping) {
      String regex = entry.first;
      if (exceptionMessage.matches(regex)) {
        return entry.second;
      }
    }
    return defaultRetryStrategy;
  }
}
