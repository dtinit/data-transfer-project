package org.datatransferproject.transport.jdk;

import com.google.inject.Injector;
import org.datatransferproject.api.launcher.ExtensionContext;
import org.datatransferproject.spi.service.extension.ServiceExtension;
import org.datatransferproject.transport.jdk.http.ReferenceApiModule;
import org.datatransferproject.transport.jdk.http.ReferenceApiServer;

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
