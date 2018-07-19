package org.datatransferproject.transport.jettyrest.http;

import org.eclipse.jetty.util.log.Logger;
import org.slf4j.LoggerFactory;

/** */
public class JettyMonitor implements Logger {
  private final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(JettyMonitor.class);

  public String getName() {
    return "Data Transfer Project";
  }

  public void warn(String msg, Object... args) {
    // TODO figure out how to handle args
  }

  public void warn(Throwable thrown) {
    warn("Error processing request", thrown);
  }

  public void warn(String msg, Throwable thrown) {
    LOGGER.warn(msg, thrown);
  }

  public void info(String msg, Object... args) {
    LOGGER.info(msg);
  }

  public void info(Throwable thrown) {
    LOGGER.info("Error processing request", thrown);
  }

  public void info(String msg, Throwable thrown) {
    LOGGER.info(msg, thrown);
  }

  public boolean isDebugEnabled() {
    return Boolean.parseBoolean(System.getProperty("jettyDebug", "false"));
  }

  public void setDebugEnabled(boolean enabled) {
    // no-op
  }

  public void debug(String msg, Object... args) {
    LOGGER.debug(msg);
  }

  public void debug(String msg, long value) {
    LOGGER.debug(msg);
  }

  public void debug(Throwable thrown) {
    LOGGER.debug("Error processing request", thrown);
  }

  public void debug(String msg, Throwable thrown) {
    LOGGER.info(msg, thrown);
  }

  public Logger getLogger(String name) {
    return this;
  }

  public void ignore(Throwable ignored) {
    // no-op
  }
}
