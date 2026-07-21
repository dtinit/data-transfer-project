package org.datatransferproject.types.common;

/** Utility class for handling exceptions. */
public class ExceptionUtils {

  /**
   * Formats a Throwable's stack trace to collapse consecutive identical frames.
   * This handles highly recursive calls (e.g. repeated copyHelper invocations)
   * by logging the first occurrence and annotating how many times it was repeated.
   */
  public static String getStackTraceAsString(Throwable t) {
    if (t == null) {
      return "";
    }
    StringBuilder sb = new StringBuilder();
    sb.append(t.toString()).append('\n');
    StackTraceElement[] trace = t.getStackTrace();
    int duplicateCount = 0;
    StackTraceElement lastElement = null;

    for (StackTraceElement element : trace) {
      if (lastElement != null && element.equals(lastElement)) {
        duplicateCount++;
      } else {
        if (duplicateCount > 0) {
          sb.append("\t... repeated ").append(duplicateCount).append(" times more\n");
          duplicateCount = 0;
        }
        sb.append("\tat ").append(element.toString()).append('\n');
        lastElement = element;
      }
    }
    if (duplicateCount > 0) {
      sb.append("\t... repeated ").append(duplicateCount).append(" times more\n");
    }

    // Now handle Caused by recursively
    Throwable cause = t.getCause();
    if (cause != null) {
      appendCause(cause, trace, sb);
    }

    return sb.toString();
  }

  private static void appendCause(Throwable cause, StackTraceElement[] enclosingTrace, StringBuilder sb) {
    sb.append("Caused by: ").append(cause.toString()).append('\n');
    StackTraceElement[] trace = cause.getStackTrace();

    // Find common suffix between trace and enclosingTrace
    int m = trace.length - 1;
    int n = enclosingTrace.length - 1;
    while (m >= 0 && n >= 0 && trace[m].equals(enclosingTrace[n])) {
      m--;
      n--;
    }
    int framesInCommon = trace.length - 1 - m;

    int duplicateCount = 0;
    StackTraceElement lastElement = null;

    for (int i = 0; i <= m; i++) {
      StackTraceElement element = trace[i];
      if (lastElement != null && element.equals(lastElement)) {
        duplicateCount++;
      } else {
        if (duplicateCount > 0) {
          sb.append("\t... repeated ").append(duplicateCount).append(" times more\n");
          duplicateCount = 0;
        }
        sb.append("\tat ").append(element.toString()).append('\n');
        lastElement = element;
      }
    }
    if (duplicateCount > 0) {
      sb.append("\t... repeated ").append(duplicateCount).append(" times more\n");
    }
    if (framesInCommon != 0) {
      sb.append("\t... ").append(framesInCommon).append(" more\n");
    }

    Throwable nextCause = cause.getCause();
    if (nextCause != null) {
      appendCause(nextCause, trace, sb);
    }
  }
}
