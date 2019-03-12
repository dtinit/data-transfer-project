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

/**
 * An interface representing a data point object, such as a timestamp or a timeseries element or
 * interval
 */
public abstract class Datum {

  // TODO: consider having all instances of Datum also keep a timestamp

  // TODO: many monitoring systems have cumulative metrics, but how do we represent that here?

  public String metricName;
  public Type type;

  public Datum(String metricName, Type type) {
    this.metricName = metricName;
    this.type = type;
  }

  public String getMetricName() {
    return metricName;
  }

  public Type getType() {
    return type;
  }

  public enum Type {
    GAUGE,      // Instantaneous measurement of a varying value, e.g., temperature
    CUMULATIVE  // Value being accumulated over time, e.g., requests since service started
  }
}