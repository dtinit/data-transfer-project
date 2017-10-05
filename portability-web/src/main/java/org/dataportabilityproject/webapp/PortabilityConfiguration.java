package org.dataportabilityproject.webapp;

import org.dataportabilityproject.ServiceProviderRegistry;
import org.dataportabilityproject.cloud.CloudFactoryFactory;
import org.dataportabilityproject.cloud.SupportedCloud;
import org.dataportabilityproject.cloud.interfaces.CloudFactory;
import org.dataportabilityproject.cloud.interfaces.PersistentKeyValueStore;
import org.dataportabilityproject.job.JobManager;
import org.dataportabilityproject.shared.Secrets;
import org.dataportabilityproject.job.IdProvider;
import org.dataportabilityproject.job.JWTTokenManager;
import org.dataportabilityproject.job.TokenManager;
import org.dataportabilityproject.job.UUIDProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PortabilityConfiguration {

  // TODO: Change this at production time.
  private static final String SECRET = "DO NOT USE IN PRODUCTION";

  @Bean
  public CloudFactory getCloudFactory(Secrets secrets) {
    // TODO: add a flag to be able to switch this.
    SupportedCloud cloud = SupportedCloud.LOCAL;
    return CloudFactoryFactory.getCloudFactory(cloud, secrets);
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

  @Bean
  public JobManager getJobManager() {
    return new JobManager(getStorage(getCloudFactory(getSecrets())), getIdProvider(), getTokenManager());
  }

    /** Provides a global singleton instance of the {@link} ServiceProviderRegistry}. */
  @Bean
  public ServiceProviderRegistry getServiceProviderRegistry(
      CloudFactory cloudFactory, Secrets secrets) {
    try {
      return new ServiceProviderRegistry(secrets, cloudFactory);
    } catch (Exception e) {
      throw new ExceptionInInitializerError(e);
    }
  }

  /** Provide file-backed implementation of secrets. */
  @Bean
  public Secrets getSecrets() {
    try {
      return new Secrets("secrets.csv");
    } catch (Exception e) {
      throw new ExceptionInInitializerError(e);
    }
  }
}
