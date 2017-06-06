package org.dataportabilityproject.webapp;

import com.google.common.base.Supplier;
import org.dataportabilityproject.ServiceProviderRegistry;
import org.dataportabilityproject.shared.Secrets;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class PortabilityServer {
  public static void main(String[] args) {
    SpringApplication.run(PortabilityServer.class, args);
  }

  /** Provides a global singleton instance of the {@link} ServiceProviderRegistry}. */
  @Bean
  ServiceProviderRegistry getServiceProviderRegistry() {
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
