package org.dataportabilityproject.gateway;

import com.google.common.collect.ImmutableSet;
import com.google.inject.AbstractModule;
import com.google.inject.multibindings.MapBinder;
import org.dataportabilityproject.spi.gateway.auth.extension.AuthServiceExtension;
import org.dataportabilityproject.spi.gateway.auth.AuthServiceProviderRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;

public class PortabilityAuthServiceProviderModule extends AbstractModule {

  private static final Logger logger =
      LoggerFactory.getLogger(PortabilityAuthServiceProviderModule.class);

  private final ImmutableSet<String> enabledServices;

  PortabilityAuthServiceProviderModule(Set<String> enabledServices) {
    this.enabledServices = ImmutableSet.copyOf(enabledServices);
  }

  @Override
  protected void configure() {
    MapBinder<String, AuthServiceExtension> mapBinder =
        MapBinder.newMapBinder(binder(), String.class, AuthServiceExtension.class);

    List<AuthServiceExtension> authServiceExtensions = new ArrayList<>();

    ServiceLoader.load(AuthServiceExtension.class)
        .iterator()
        .forEachRemaining(authServiceExtensions::add);

    for (AuthServiceExtension provider : authServiceExtensions) {
      if (enabledServices.contains(provider.getServiceId())) {
        logger.debug("Found AuthServiceExtension: {}", provider.getServiceId());
        mapBinder.addBinding(provider.getServiceId()).to(provider.getClass());
      }
    }

    bind(AuthServiceProviderRegistry.class).to(PortabilityAuthServiceProviderRegistry.class);
  }
}
