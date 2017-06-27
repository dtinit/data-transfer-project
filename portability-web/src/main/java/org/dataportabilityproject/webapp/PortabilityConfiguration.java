package org.dataportabilityproject.webapp;

import org.dataportabilityproject.ServiceProviderRegistry;
import org.dataportabilityproject.cloud.interfaces.CloudFactory;
import org.dataportabilityproject.cloud.interfaces.PersistentKeyValueStore;
import org.dataportabilityproject.cloud.local.LocalCloudFactory;
import org.dataportabilityproject.shared.Secrets;
import org.dataportabilityproject.webapp.auth.IdProvider;
import org.dataportabilityproject.webapp.auth.JWTTokenManager;
import org.dataportabilityproject.webapp.auth.TokenManager;
import org.dataportabilityproject.webapp.auth.UUIDProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PortabilityConfiguration {

  // TODO: Change this at production time.
  private static final String SECRET = "DO NOT USE IN PRODUCTION";

  @Bean
  public CloudFactory getCloudFactory() {
    // TODO(willard): Add a flag to control the cloud environment.
    return new LocalCloudFactory();
  }

  @Bean
  public PersistentKeyValueStore getStorage(CloudFactory cloudFactory) {
    return cloudFactory.getPersistentKeyValueStore();
  }

  @Bean
  public IdProvider getIdProvider() {
    return new UUIDProvider();
  }

  @Bean
  public TokenManager getTokenManager() {
    return new JWTTokenManager(SECRET);
  }

    /** Provides a global singleton instance of the {@link} ServiceProviderRegistry}. */
  @Bean
  public ServiceProviderRegistry getServiceProviderRegistry(CloudFactory cloudFactory) {
    try {
      return new ServiceProviderRegistry(getSecrets(), cloudFactory);
    } catch (Exception e) {
      throw new ExceptionInInitializerError(e);
    }
  }

  /** Provide file-backed implementation of secrets. */
  private static Secrets getSecrets() {
    try {
      return new Secrets("secrets.csv");
    } catch (Exception e) {
      throw new ExceptionInInitializerError(e);
    }
  }
}
