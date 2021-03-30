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

import java.util.UUID;
import org.datatransferproject.api.launcher.Monitor;

import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.function.Supplier;
import org.datatransferproject.launcher.monitor.events.EventCode;

/** Outputs monitor events to the console. Uses ANSI color codes in shells that support them. */
public class ConsoleMonitor implements Monitor {
  private Level level;

  public enum Level {
    SEVERE(2),
    INFO(1),
    DEBUG(0);

    int value;

    Level(int value) {
      this.value = value;
    }
  }

  private static final String ANSI_RESET = "\u001B[0m";
  private static final String ANSI_BLACK = "\u001B[30m";
  private static final String ANSI_RED = "\u001B[31m";
  private static final String ANSI_BLUE = "\u001B[34m";

  private boolean ansi;

  public ConsoleMonitor(Level level) {
    this.level = level;
    ansi = !System.getProperty("os.name").contains("Windows");
  }

  public void severe(Supplier<String> supplier, Object... data) {
    output("SEVERE", supplier, ANSI_RED, data);
  }

  public void info(Supplier<String> supplier, Object... data) {
    if (Level.INFO.value < level.value) {
      return;
    }
    output("INFO", supplier, ANSI_BLUE, data);
  }

  public void debug(Supplier<String> supplier, Object... data) {
    if (Level.DEBUG.value < level.value) {
      return;
    }
    output("DEBUG", supplier, ANSI_BLACK, data);
  }

  private void output(String level, Supplier<String> supplier, String color, Object... data) {
    color = ansi ? color : "";
    String reset = ansi ? ANSI_RESET : "";

    String time = ZonedDateTime.now(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    System.out.println(color + level + " " + time + " " + supplier.get() + reset);
    if (data != null) {
      for (Object datum : data) {
        if (datum instanceof Throwable) {
          ((Throwable) datum).printStackTrace(System.out);
        } else if (datum instanceof UUID) {
          System.out.println("JobId: " + ((UUID)datum).toString());
        } else if (datum instanceof EventCode) {
          System.out.println("EventCode: " + datum.toString());
        } else if (datum != null) {
          System.out.println(datum);
        }
      }
    }
  }
}
