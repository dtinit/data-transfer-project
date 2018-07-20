package org.datatransferproject.transfer.logging;

import com.google.common.base.Preconditions;
import java.util.UUID;
import org.apache.log4j.Layout;
import org.apache.log4j.helpers.ISO8601DateFormat;
import org.apache.log4j.spi.LoggingEvent;

public class EncryptingLayout extends Layout {

  static UUID jobId;

  public static void setJobId(UUID inputJobId) {
    Preconditions.checkState(jobId == null, "jobId has already been set");
    jobId = inputJobId;
  }

  @Override
  public String format(LoggingEvent event) {
    // NB: copied from SimpleLayout.format()
    return String.format("[%s] [%s]: %s - %s%s",
        new ISO8601DateFormat().format(event.timeStamp),
        jobId != null ? Long.toHexString(jobId.getMostSignificantBits()) : "undefined",
        event.getLevel().toString(),
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
