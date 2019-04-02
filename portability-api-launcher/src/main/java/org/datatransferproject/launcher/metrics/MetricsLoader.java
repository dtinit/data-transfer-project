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
package org.datatransferproject.launcher.metrics;

import org.datatransferproject.api.launcher.DtpInternalMetricRecorder;
import org.datatransferproject.api.launcher.MetricExtension;
import org.datatransferproject.launcher.monitor.MonitorLoader;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

/** Helper for loading metric extensions. */
public class MetricsLoader {
  private static DtpInternalMetricRecorder metricRecorder;
  public static synchronized DtpInternalMetricRecorder loadMetrics() {
    if (metricRecorder == null) {
      try {
        List<DtpInternalMetricRecorder> metricRecorders = new ArrayList<>();
        ServiceLoader.load(MetricExtension.class)
            .iterator()
            .forEachRemaining(
                extension -> {
                  try {
                  extension.initialize();
                    metricRecorders.add(extension.getMetricRecorder());
                  } catch (Throwable e) {
                    System.out.println("Couldn't initialize: " + extension + ": " + e.getMessage());
                    e.printStackTrace(System.out);
                  }
                });
        if (metricRecorders.isEmpty()) {
          metricRecorder = new LoggingDtpInternalMetricRecorder(MonitorLoader.loadMonitor());
        } else if (metricRecorders.size() == 1) {
          metricRecorder = metricRecorders.get(0);
        } else {
          DtpInternalMetricRecorder[] array = metricRecorders.toArray(
              new DtpInternalMetricRecorder[metricRecorders.size()]);
          metricRecorder = new MultiplexMetricRecorder(array);
        }
      } catch (Throwable t) {
        t.printStackTrace();
        throw t;
      }
    }
    return metricRecorder;
  }

  private MetricsLoader() {}
}
