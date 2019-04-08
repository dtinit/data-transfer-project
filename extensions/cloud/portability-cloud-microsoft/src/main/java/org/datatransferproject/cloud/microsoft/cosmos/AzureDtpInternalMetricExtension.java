package org.datatransferproject.cloud.microsoft.cosmos;

import org.datatransferproject.api.launcher.DtpInternalMetricRecorder;
import org.datatransferproject.api.launcher.ExtensionContext;
import org.datatransferproject.spi.service.extension.ServiceExtension;

public class AzureDtpInternalMetricExtension implements ServiceExtension {

  @Override
  public void initialize(ExtensionContext context) {
    context.registerService(
        DtpInternalMetricRecorder.class,
        new AzureDtpInternalMetricRecorder(context.getMonitor()));
  }
}
