package org.dataportabilityproject.transport.jdk;

import com.google.inject.Injector;
import org.dataportabilityproject.api.launcher.ExtensionContext;
import org.dataportabilityproject.spi.service.extension.ServiceExtension;
import org.dataportabilityproject.transport.jdk.http.ReferenceApiModule;
import org.dataportabilityproject.transport.jdk.http.ReferenceApiServer;

/** A transport implementation based on the default JDK http library. */
public class JdkTransportExtension implements ServiceExtension {

  private ReferenceApiServer apiServer;
  private ExtensionContext context;

  @Override
  public void initialize(ExtensionContext context) {
    this.context = context;
  }

  @Override
  public void start() {
    try {
      Injector injector =
          context.getService(Injector.class).createChildInjector(new ReferenceApiModule());
      apiServer = injector.getInstance(ReferenceApiServer.class);
      apiServer.start();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void shutdown() {
    apiServer.stop();
  }
}
