package org.datatransferproject.transfer.logging;

import java.util.UUID;
import org.apache.log4j.Layout;
import org.apache.log4j.helpers.ISO8601DateFormat;
import org.apache.log4j.spi.LoggingEvent;

public class EncryptingLayout extends Layout {

  private final UUID jobId;

  public EncryptingLayout(UUID jobId) {
    this.jobId = jobId;
  }

  @Override
  public String format(LoggingEvent event) {
    // NB: copied from SimpleLayout.format()
    return String.format("[%s] [%s]: %s - %s%s", new ISO8601DateFormat().format(event.timeStamp),
        jobId, event.getLevel().toString(),
        event.getRenderedMessage(), LINE_SEP);
  }

  @Override
  public boolean ignoresThrowable() {
    return true;
  }

  @Override
  public void activateOptions() {
  }
}
