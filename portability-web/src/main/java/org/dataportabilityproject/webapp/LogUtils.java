package org.dataportabilityproject.webapp;

/** Utility methods for logging. */
public class LogUtils {

  // TODO: Replace with logging framework
  public static void log (String fmt, Object... args) {
    System.out.println(String.format(fmt, args));
  }
}
