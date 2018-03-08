package org.dataportabilityproject.gateway;

import com.google.common.collect.ImmutableSet;
import com.google.inject.AbstractModule;
import com.google.inject.multibindings.MapBinder;
import org.dataportabilityproject.spi.gateway.auth.AuthServiceProvider;
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
    MapBinder<String, AuthServiceProvider> mapBinder =
        MapBinder.newMapBinder(binder(), String.class, AuthServiceProvider.class);

    List<AuthServiceProvider> authServiceProviders = new ArrayList<>();

    ServiceLoader.load(AuthServiceProvider.class)
        .iterator()
        .forEachRemaining(authServiceProviders::add);

    for (AuthServiceProvider provider : authServiceProviders) {
      if (enabledServices.contains(provider.getServiceId())) {
        logger.debug("Found AuthServiceProvider: {}", provider.getServiceId());
        mapBinder.addBinding(provider.getServiceId()).to(provider.getClass());
      }
    }

    bind(AuthServiceProviderRegistry.class).to(PortabilityAuthServiceProviderRegistry.class);
  }
}
