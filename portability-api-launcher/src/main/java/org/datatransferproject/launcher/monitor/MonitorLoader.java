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
package org.datatransferproject.launcher.monitor;

import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.api.launcher.MonitorExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

import static org.datatransferproject.launcher.monitor.ConsoleMonitor.Level.DEBUG;

/** Helper for loading monitor extensions. */
public class MonitorLoader {
  private static Monitor monitor;
  public static synchronized Monitor loadMonitor() {
    if (monitor == null) {
      try {
        List<Monitor> monitors = new ArrayList<>();
        ServiceLoader.load(MonitorExtension.class)
            .iterator()
            .forEachRemaining(
                extension -> {
                  try {
                  extension.initialize();
                  monitors.add(extension.getMonitor());
                  } catch (Throwable e) {
                    System.out.println("Couldn't initialize: " + extension + ": " + e.getMessage());
                    e.printStackTrace(System.out);
                  }
                });
        if (monitors.isEmpty()) {
          monitor = new ConsoleMonitor(DEBUG);
        } else if (monitors.size() == 1) {
          monitor = monitors.get(0);
        } else {
          Monitor[] monitorArray = new Monitor[monitors.size()];
          monitorArray = monitors.toArray(monitorArray);
          monitor = new MultiplexMonitor(monitorArray);
        }
      } catch (Throwable t) {
        t.printStackTrace();
        throw t;
      }
    }
    return monitor;
  }

  private MonitorLoader() {}
}
