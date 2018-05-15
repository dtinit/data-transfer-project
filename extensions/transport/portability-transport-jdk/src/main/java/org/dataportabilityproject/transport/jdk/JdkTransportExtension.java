package org.dataportabilityproject.transport.jdk;

import com.google.inject.Injector;
import org.dataportabilityproject.api.launcher.ExtensionContext;
import org.dataportabilityproject.spi.service.extension.ServiceExtension;
import org.dataportabilityproject.transport.jdk.http.ReferenceApiModule;
import org.dataportabilityproject.transport.jdk.http.ReferenceApiServer;

/** */
public class JdkTransportExtension implements ServiceExtension {

  private ReferenceApiServer apiServer;
  private ExtensionContext context;

  @Override
  public void initialize(ExtensionContext context) {
    // context.registerService(TransportBinder.class, binder);
    this.context = context;
  }

  @Override
  public void start() {
    try {
      Injector injector =
          context.getService(Injector.class).createChildInjector(new ReferenceApiModule(context));
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
