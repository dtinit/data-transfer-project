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

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.base.Throwables.getStackTraceAsString;

import java.util.UUID;
import org.datatransferproject.api.launcher.Monitor;

import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.function.Supplier;
import org.datatransferproject.launcher.monitor.events.EventCode;

/**
 * Outputs monitor events to the console. Uses ANSI color codes in shells that support them.
 *
 * <p>For pretty color output in your local TTY ensure your shell has set
 * {@code FORCE_COLOR} environment variable to "1" or "true" (and that you're
 * not on Windows).
 */
public class ConsoleMonitor implements Monitor {
  private final Level minLevel;
  private final boolean useAnsiColor = shouldUseColor();

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

  /** Constructs a logger that drops all logs at a level below {@code minLevel}. */
  public ConsoleMonitor(Level minLevel) {
    this.minLevel = minLevel;
  }

  public void severe(Supplier<String> supplier, Object... data) {
    output("SEVERE", supplier, ANSI_RED, data);
  }

  public void info(Supplier<String> supplier, Object... data) {
    if (Level.INFO.value < minLevel.value) {
      return;
    }
    output("INFO", supplier, ANSI_BLUE, data);
  }

  public void debug(Supplier<String> supplier, Object... data) {
    if (Level.DEBUG.value < minLevel.value) {
      return;
    }
    output("DEBUG", supplier, ANSI_BLACK, data);
  }

  private void output(String level, Supplier<String> supplier, String color, Object... data) {
    StringBuilder builder = new StringBuilder();
    if (useAnsiColor) {
      builder.append(color);
    }

    builder.append(level);
    builder.append(" ");

    // ISO, because obvz (sortable, standard), and offset because this may be
    // shared/discussed/debugged outside someone's console, at which point a
    // vague clock time without its timezone is useless.
    builder.append(ZonedDateTime.now(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));

    builder.append(" ");
    builder.append(supplier.get());

    if (useAnsiColor) {
      builder.append(ANSI_RESET);
    }

    if (data != null) {
      for (Object datum : data) {
        if (datum instanceof Throwable) {
          builder.append(getStackTraceAsString((Throwable) datum));
        } else if (datum instanceof UUID) {
          builder.append("JobId: ");
          builder.append(((UUID) datum).toString());
        } else if (datum instanceof EventCode) {
          builder.append("EventCode: ");
          builder.append(((EventCode) datum).toString());
        } else if (datum != null) {
          builder.append(datum);
        }
      }
    }

    // Write to standard error, as these are debug logs for which any buffering
    // won't help us at all.
    System.err.println(builder.toString());
  }

  /**
   * Whether we think we're attached to a human's TTY that wants pretty colors,
   * otherwise this is a log file someone may be hunting/analyzing later so
   * ASCII-escape codes for color will just be annoying.
   */
  private static final boolean shouldUseColor() {
    try {
      final String osName = System.getProperty("os.name");
      return getEnv("FORCE_COLOR") && !getEnv("NO_COLOR") &&
        !osName.contains("Windows");
    } catch (Exception e) {
      return false;
    }
  }

  /** Check of boolean-esque env. variable, or false if anything goes wrong. */
  private static final boolean getEnv(String envVarName) {
    try {
      final String rawEnvValue = System.getenv(envVarName);
      if (isNullOrEmpty(rawEnvValue)) {
        return false;
      }
      return rawEnvValue.equals("1") || rawEnvValue.equals("true");
    } catch (Exception e) {
      return false;
    }
  }
}
