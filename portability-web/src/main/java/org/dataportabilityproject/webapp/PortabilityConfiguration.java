/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.dataportabilityproject.webapp;

import org.dataportabilityproject.PortabilityFlags;
import org.dataportabilityproject.ServiceProviderRegistry;
import org.dataportabilityproject.cloud.CloudFactoryFactory;
import org.dataportabilityproject.cloud.SupportedCloud;
import org.dataportabilityproject.cloud.interfaces.CloudFactory;
import org.dataportabilityproject.cloud.interfaces.PersistentKeyValueStore;
import org.dataportabilityproject.job.Crypter;
import org.dataportabilityproject.job.IdProvider;
import org.dataportabilityproject.job.JWTTokenManager;
import org.dataportabilityproject.job.JobDao;
import org.dataportabilityproject.job.PortabilityJobFactory;
import org.dataportabilityproject.job.TokenManager;
import org.dataportabilityproject.job.UUIDProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PortabilityConfiguration {

  // TODO: Change this at production time.
  private static final String SECRET = "DO NOT USE IN PRODUCTION";

  @Bean
  public CloudFactory getCloudFactory() {
    SupportedCloud cloud = PortabilityFlags.cloud();
    return CloudFactoryFactory.getCloudFactory(cloud);
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
  public JobDao getJobDao() {
    return new JobDao(getStorage(getCloudFactory()));
  }

  @Bean
  public PortabilityJobFactory getJobFactory() {
    return new PortabilityJobFactory(getIdProvider());
  }

  @Bean
  public CryptoHelper getCryptoHelper() {
    return new CryptoHelper(new Crypter(){}); // TODO: Wire up correct Crypter
  }

    /** Provides a global singleton instance of the {@link} ServiceProviderRegistry}. */
  @Bean
  public ServiceProviderRegistry getServiceProviderRegistry(CloudFactory cloudFactory) {
    try {
      return new ServiceProviderRegistry(cloudFactory);
    } catch (Exception e) {
      throw new ExceptionInInitializerError(e);
    }
  }
}
