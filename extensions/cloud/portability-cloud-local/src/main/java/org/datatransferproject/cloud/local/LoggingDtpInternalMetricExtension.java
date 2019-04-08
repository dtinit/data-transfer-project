package org.datatransferproject.cloud.local;

import org.datatransferproject.api.launcher.DtpInternalMetricRecorder;
import org.datatransferproject.api.launcher.ExtensionContext;
import org.datatransferproject.spi.service.extension.ServiceExtension;

public class LoggingDtpInternalMetricExtension implements ServiceExtension {

  @Override
  public void initialize(ExtensionContext context) {
    context.registerService(
        DtpInternalMetricRecorder.class,
        new LoggingDtpInternalMetricRecorder(context.getMonitor()));
  }
}
