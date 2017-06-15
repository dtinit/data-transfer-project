package org.dataportabilityproject.webapp;

import com.google.common.base.Supplier;
import org.dataportabilityproject.ServiceProviderRegistry;
import org.dataportabilityproject.shared.Secrets;
import org.dataportabilityproject.webapp.auth.IdProvider;
import org.dataportabilityproject.webapp.auth.JWTTokenManager;
import org.dataportabilityproject.webapp.auth.TokenManager;
import org.dataportabilityproject.webapp.auth.UUIDProvider;
import org.dataportabilityproject.webapp.storage.InMemoryStorage;
import org.dataportabilityproject.webapp.storage.Storage;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PortabilityConfiguration {

  // TODO: Change this at production time.
  private static final String SECRET = "DO NOT USE IN PRODUCTION";

  @Bean
  public Storage getStorage() {return new InMemoryStorage(); }

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
  public ServiceProviderRegistry getServiceProviderRegistry() {
    try {
      return new ServiceProviderRegistry(getSecretsSupplier());
    } catch (Exception e) {
      throw new ExceptionInInitializerError(e);
    }
  }

  /** Provide file-backed implementation of secrets supplier. */
  private static Supplier<Secrets> getSecretsSupplier() {
    return new Supplier<Secrets>() {
      @Override
      public Secrets get() {
        try {
          return new Secrets("secrets.csv");
        } catch (Exception e) {
          throw new ExceptionInInitializerError(e);
        }
      }
    };
  }
}
