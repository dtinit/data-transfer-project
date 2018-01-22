package org.dataportabilityproject.worker;

import com.google.inject.AbstractModule;
import org.dataportabilityproject.PortabilityCoreModule;

public class WorkerModule extends AbstractModule {

  @Override
  protected void configure() {
    install(new PortabilityCoreModule());
  }
}
