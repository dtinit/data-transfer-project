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
package org.datatransferproject.api.launcher;

import java.time.Duration;

/**
 * Interface to allow transfer extensions to record generic metrics/stats about their processing.
 */
public interface MetricRecorder {

  /**
   * Record a generic event metric.
   *
   * @param dataType the data type being processed
   * @param tag a tag to identify the metric, this should be a low cardinality value
   */
  void recordMetric(String dataType, String tag);

  /**
   * Record a generic event metric with a boolean value.
   *
   * @param dataType the data type being processed
   * @param tag a tag to identify the metric, this should be a low cardinality value
   * @param bool a true/false value related to the event
   */
  void recordMetric(String dataType, String tag, boolean bool);

  /**
   * Record a generic event metric with a boolean value.
   *
   * @param dataType the data type being processed
   * @param tag a tag to identify the metric, this should be a low cardinality value
   * @param duration a duration related to the event
   */
  void recordMetric(String dataType, String tag, Duration duration);

  /**
   * Record a generic event metric with a integer value.
   *
   * @param dataType the data type being processed
   * @param tag a tag to identify the metric, this should be a low cardinality value
   * @param value a numeric value related to the event
   */
  void recordMetric(String dataType, String tag, int value);
}
