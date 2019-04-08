package org.datatransferproject.cloud.google;

import org.datatransferproject.api.launcher.DtpInternalMetricRecorder;
import org.datatransferproject.api.launcher.ExtensionContext;
import org.datatransferproject.spi.service.extension.ServiceExtension;

public class GoogleDtpInternalMetricExtension implements ServiceExtension {

  @Override
  public void initialize(ExtensionContext context) {
    context.registerService(
        DtpInternalMetricRecorder.class,
        new GoogleDtpInternalMetricRecorder(context.getMonitor()));
  }
}
