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

  public static Monitor loadMonitor() {
    List<Monitor> monitors = new ArrayList<>();
    ServiceLoader.load(MonitorExtension.class)
        .iterator()
        .forEachRemaining(
            extension -> {
              extension.getMonitor();
              extension.initialize();
              monitors.add(extension.getMonitor());
            });
    if (monitors.isEmpty()) {
      return new ConsoleMonitor(DEBUG);
    } else {
      Monitor[] monitorArray = new Monitor[monitors.size()];
      monitorArray = monitors.toArray(monitorArray);
      return new MultiplexMonitor(monitorArray);
    }
  }

  private MonitorLoader() {}
}
