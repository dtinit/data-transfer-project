/*
 * Copyright 2018 The Data Transfer Project Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.dataportabilityproject.gateway;

import com.google.common.base.Preconditions;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.multibindings.MapBinder;
import java.io.IOException;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import org.dataportabilityproject.api.launcher.ExtensionContext;
import org.dataportabilityproject.api.launcher.TypeManager;
import org.dataportabilityproject.gateway.reference.ReferenceApiModule;
import org.dataportabilityproject.gateway.reference.ReferenceApiServer;
import org.dataportabilityproject.launcher.impl.TypeManagerImpl;
import org.dataportabilityproject.security.AesSymmetricKeyGenerator;
import org.dataportabilityproject.security.SymmetricKeyGenerator;
import org.dataportabilityproject.spi.cloud.extension.CloudExtension;
import org.dataportabilityproject.spi.cloud.storage.AppCredentialStore;
import org.dataportabilityproject.spi.cloud.storage.JobStore;
import org.dataportabilityproject.spi.gateway.auth.AuthServiceProviderRegistry;
import org.dataportabilityproject.spi.gateway.auth.extension.AuthServiceExtension;
import org.dataportabilityproject.spi.service.extension.ServiceExtension;
import org.dataportabilityproject.types.transfer.auth.AppCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Starts the api server. */
public class ApiMain {
  private static final Logger logger = LoggerFactory.getLogger(ApiMain.class);

  private ReferenceApiServer server;

  /** Starts the api server, currently the reference implementation. */
  public static void main(String[] args) throws Exception {
    logger.warn("Starting reference api server.");
    Thread.setDefaultUncaughtExceptionHandler(
        new UncaughtExceptionHandler() {
          @Override
          public void uncaughtException(Thread thread, Throwable t) {
            logger.warn("Uncaught exception in thread: {}", thread.getName(), t);
          }
        });

    ApiMain apiMain = new ApiMain();
    apiMain.initializeHttp();
    apiMain.start();
  }

  public void initializeHttp() {
    initializeHttps(null, null);
  }

  public void initializeHttps(
      TrustManagerFactory trustManagerFactory, KeyManagerFactory keyManagerFactory) {
    TypeManager typeManager = new TypeManagerImpl();

    // TODO implement - dont hardcode.
    Map<String, Object> configuration = new HashMap<>();
    configuration.put("cloud", "GOOGLE");

    ApiExtensionContext extensionContext = new ApiExtensionContext(typeManager, configuration);

    // Services that need to be shared between authServiceExtensions or load types in the
    // typemanager get initialized first.
    ServiceLoader.load(ServiceExtension.class)
        .iterator()
        .forEachRemaining(serviceExtension -> serviceExtension.initialize(extensionContext));

    CloudExtension cloudExtension = getCloudExtension();
    cloudExtension.initialize(extensionContext);

    // TODO: for now, use the same context as the cloud extension and modify it. Later we should
    // create a separate extension.
    extensionContext.addService(AppCredentialStore.class, cloudExtension.getAppCredentialStore());
    extensionContext.addService(JobStore.class, cloudExtension.getJobStore());

    // TODO: Load up only "enabled" services
    List<AuthServiceExtension> authServiceExtensions = new ArrayList<>();
    ServiceLoader.load(AuthServiceExtension.class)
        .iterator()
        .forEachRemaining(
            (authServiceExtension) -> {
              authServiceExtension.initialize(extensionContext);
              authServiceExtensions.add(authServiceExtension);
            });

    // TODO: make configurable
    SymmetricKeyGenerator keyGenerator = new AesSymmetricKeyGenerator();

    JobStore jobStore = cloudExtension.getJobStore();
    Injector injector =
        Guice.createInjector(
            new ApiServicesModule(
                typeManager,
                jobStore,
                keyGenerator,
                trustManagerFactory,
                keyManagerFactory,
                authServiceExtensions),
            new ReferenceApiModule());

    // Launch the application
    // TODO: Support other server implementations, e.g. Jetty, Tomcat
    server = injector.getInstance(ReferenceApiServer.class);
  }

  public void start() throws Exception {
    server.start();
  }

  public void stop() {
    if (server != null) {
      server.stop();
    }
  }

  public void shutdown() {
    // not currently used but in the future it may be to allow HTTP servers to be temporarily paused
  }

  private CloudExtension getCloudExtension() {
    List<CloudExtension> cloudExtensions = new ArrayList<>();
    ServiceLoader.load(CloudExtension.class).iterator().forEachRemaining(cloudExtensions::add);
    if (cloudExtensions.isEmpty()) {
      throw new IllegalStateException(
          "A cloud extension is not available. Exactly one is required.");
    } else if (cloudExtensions.size() > 1) {
      throw new IllegalStateException(
          "Multiple cloud authServiceExtensions were found. Exactly one is required.");
    }

    return cloudExtensions.get(0);
  }

  private class ApiExtensionContext implements ExtensionContext {
    private final TypeManager typeManager;
    private final Map<String, Object> configuration;
    private final Map<Class, Object> serviceMap = new HashMap<>();

    public ApiExtensionContext(TypeManager typeManager, Map<String, Object> configuration) {
      this.typeManager = typeManager;
      this.configuration = configuration;
    }

    public <T> void addService(Class<T> type, T object) {
      serviceMap.put(type, object);
    }

    @Override
    public org.dataportabilityproject.api.launcher.Logger getLogger() {
      // TODO implement
      return null;
    }

    @Override
    public TypeManager getTypeManager() {
      return typeManager;
    }

    @Override
    public <T> T getService(Class<T> type) {
      return (T)serviceMap.get(type);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getConfiguration(final String key, final T defaultValue) {
      return (T) configuration.getOrDefault(key, defaultValue);
    }
  }

  private class ApiServicesModule extends AbstractModule {
    private final TypeManager typeManager;
    private final JobStore jobStore;
    private final SymmetricKeyGenerator keyGenerator;
    private final List<AuthServiceExtension> authServiceExtensions;
    private final TrustManagerFactory trustManagerFactory;
    private final KeyManagerFactory keyManagerFactory;

    public ApiServicesModule(
        TypeManager typeManager,
        JobStore jobStore,
        SymmetricKeyGenerator keyGenerator,
        TrustManagerFactory trustManagerFactory,
        KeyManagerFactory keyManagerFactory,
        List<AuthServiceExtension> authServiceExtensions) {
      this.typeManager = typeManager;
      this.jobStore = jobStore;
      this.keyGenerator = keyGenerator;
      this.authServiceExtensions = authServiceExtensions;
      this.trustManagerFactory = trustManagerFactory;
      this.keyManagerFactory = keyManagerFactory;

      if (trustManagerFactory != null || keyManagerFactory != null) {
        Preconditions.checkNotNull(
            trustManagerFactory,
            "If a key manager factory is specified, a trust manager factory must also be provided");
        Preconditions.checkNotNull(
            keyManagerFactory,
            "If a trust manager factory  is specified, a key manager factory must also be provided");
      }
    }

    @Override
    protected void configure() {
      MapBinder<String, AuthServiceExtension> mapBinder =
          MapBinder.newMapBinder(binder(), String.class, AuthServiceExtension.class);

      authServiceExtensions
          .stream()
          .forEach(
              authExtension ->
                  mapBinder.addBinding(authExtension.getServiceId()).to(authExtension.getClass()));

      bind(AuthServiceProviderRegistry.class).to(PortabilityAuthServiceProviderRegistry.class);
      bind(SymmetricKeyGenerator.class).toInstance(keyGenerator);
      bind(TypeManager.class).toInstance(typeManager);
      bind(JobStore.class).toInstance(jobStore);
      if (trustManagerFactory != null) {
        bind(TrustManagerFactory.class).toInstance(trustManagerFactory);
      }
      if (keyManagerFactory != null) {
        bind(KeyManagerFactory.class).toInstance(keyManagerFactory);
      }
    }
  }
}
