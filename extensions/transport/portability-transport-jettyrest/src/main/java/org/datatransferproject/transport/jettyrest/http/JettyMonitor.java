package org.datatransferproject.transport.jettyrest.http;

import org.datatransferproject.api.launcher.Monitor;
import org.eclipse.jetty.util.log.Logger;

/** */
public class JettyMonitor implements Logger {
  private static Monitor DELEGATE;

  public String getName() {
    return "Data Transfer Project";
  }

  public static void setDelegate(Monitor delegate) {
    JettyMonitor.DELEGATE = delegate;
  }

  public void warn(String msg, Object... args) {
    // TODO figure out how to handle args
  }

  public void warn(Throwable thrown) {
    warn("Error processing request", thrown);
  }

  public void warn(String msg, Throwable thrown) {
    DELEGATE.info(() -> msg, thrown);
  }

  public void info(String msg, Object... args) {
    DELEGATE.info(() -> msg);
  }

  public void info(Throwable thrown) {
    DELEGATE.info(() -> "Error processing request", thrown);
  }

  public void info(String msg, Throwable thrown) {
    DELEGATE.info(() -> msg, thrown);
  }

  public boolean isDebugEnabled() {
    return Boolean.parseBoolean(System.getProperty("jettyDebug", "false"));
  }

  public void setDebugEnabled(boolean enabled) {
    // no-op
  }

  public void debug(String msg, Object... args) {
    DELEGATE.debug(() -> msg);
  }

  public void debug(String msg, long value) {
    DELEGATE.debug(() -> msg);
  }

  public void debug(Throwable thrown) {
    DELEGATE.debug(() -> "Error processing request", thrown);
  }

  public void debug(String msg, Throwable thrown) {
    DELEGATE.info(() -> msg, thrown);
  }

  public Logger getLogger(String name) {
    return this;
  }

  public void ignore(Throwable ignored) {
    // no-op
  }
}
