package org.datatransferproject.transfer.logging;

import org.apache.log4j.Layout;
import org.apache.log4j.spi.LoggingEvent;

public class EncryptingLayout extends Layout {
  @Override
  public String format(LoggingEvent event) {
    // NB: copied from SimpleLayout.format()
    StringBuilder builder = new StringBuilder();
    builder.append(event.getLevel().toString());
    builder.append(" - ");
    builder.append(event.getRenderedMessage());
    builder.append(LINE_SEP);
    return builder.toString();
  }

  @Override
  public boolean ignoresThrowable() {
    return true;
  }

  @Override
  public void activateOptions() {
  }
}
