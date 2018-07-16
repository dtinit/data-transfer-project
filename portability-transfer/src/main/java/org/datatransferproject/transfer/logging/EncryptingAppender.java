package org.datatransferproject.transfer.logging;

import com.google.common.annotations.VisibleForTesting;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.UUID;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.helpers.QuietWriter;
import org.apache.log4j.spi.ErrorHandler;
import org.apache.log4j.spi.Filter;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;

@Plugin(name = "Encrypting", category = "Core", elementType = "appender", printObject = true)
public class EncryptingAppender extends AppenderSkeleton {
  private UUID jobId;
  private EncryptingLayout layout;
  private QuietWriter quietWriter;

  private static volatile EncryptingAppender instance;

  public EncryptingAppender(EncryptingLayout layout, QuietWriter writer) {
    this.layout = layout;
    this.quietWriter = writer;
  }

  @VisibleForTesting
  EncryptingAppender(EncryptingLayout layout, OutputStream outputStream, ErrorHandler errorHandler) {
    this.layout = layout;
    this.quietWriter = new QuietWriter(new OutputStreamWriter(outputStream), errorHandler);
  }

  public void setJobId(UUID jobId) {
    this.jobId = jobId;
  }

  @Override
  protected void append(LoggingEvent event) {
    quietWriter.write(String.format("[%s]: %", jobId, layout.format(event)));
  }

  @Override
  public void close() {

  }

  @Override
  public boolean requiresLayout() {
    return false;
  }

  // TODO: check this!  This feels wrong!
  @PluginFactory
  public static EncryptingAppender createAppender(@PluginAttribute("name") String name,
      @PluginAttribute("ignoreExceptions") boolean ignoreExceptions,
      @PluginElement("Layout") EncryptingLayout layout,
      @PluginElement("QuietWriter") QuietWriter writer,
      @PluginElement("Filters") Filter filter) {

    instance = new EncryptingAppender(layout, writer);
    return instance;
  }
}
