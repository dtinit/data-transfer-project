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
import org.dataportabilityproject.api.launcher.ExtensionContext;
import org.dataportabilityproject.api.launcher.TypeManager;
import org.dataportabilityproject.gateway.reference.ReferenceApiModule;
import org.dataportabilityproject.gateway.reference.ReferenceApiServer;
import org.dataportabilityproject.launcher.impl.TypeManagerImpl;
import org.dataportabilityproject.security.AesSymmetricKeyGenerator;
import org.dataportabilityproject.security.SymmetricKeyGenerator;
import org.dataportabilityproject.spi.cloud.extension.CloudExtension;
import org.dataportabilityproject.spi.cloud.storage.JobStore;
import org.dataportabilityproject.spi.gateway.auth.AuthServiceProviderRegistry;
import org.dataportabilityproject.spi.gateway.auth.extension.AuthServiceExtension;
import org.dataportabilityproject.spi.service.extension.ServiceExtension;
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
    apiMain.initialize();
    apiMain.start();
  }

  public void initialize() {
    TypeManager typeManager = new TypeManagerImpl();

    // TODO implement
    Map<String, Object> configuration = new HashMap<>();

    // TODO: configure from command line - for now this is hardcoded to google cloud.
    configuration.put("cloud", "GOOGLE");

    ExtensionContext extensionContext = new ApiExtensionContext(typeManager, configuration);

    // Services that need to be shared between extensions or load types in the typemanager get
    // initialized first.
    ServiceLoader.load(ServiceExtension.class)
        .iterator()
        .forEachRemaining(serviceExtension -> serviceExtension.initialize(extensionContext));

    CloudExtension cloudExtension = getCloudExtension();
    cloudExtension.initialize(extensionContext);

    // TODO: Load up only "enabled" services
    List<AuthServiceExtension> authServiceExtensions = new ArrayList<>();
    ServiceLoader.load(AuthServiceExtension.class)
        .iterator()
        .forEachRemaining(
            (serviceExtension) -> {
              serviceExtension.initialize(extensionContext);
              authServiceExtensions.add(serviceExtension);
            });

    // TODO: make configurable
    SymmetricKeyGenerator keyGenerator = new AesSymmetricKeyGenerator();

    Injector injector =
        Guice.createInjector(
            new ApiServicesModule(
                typeManager, cloudExtension.getJobStore(), keyGenerator, authServiceExtensions),
            new ReferenceApiModule());

    // Launch the application
    // TODO: Support other server implementations, e.g. Jetty, Tomcat
    server = injector.getInstance(ReferenceApiServer.class);
  }

  public void start() throws IOException {
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
          "Multiple cloud extensions were found. Exactly one is required.");
    }

    return cloudExtensions.get(0);
  }

  private class ApiExtensionContext implements ExtensionContext {
    private final TypeManager typeManager;
    private final Map<String, Object> configuration;
    private final Map<Class, Object> services = new HashMap<>();

    public ApiExtensionContext(TypeManager typeManager, Map<String, Object> configuration) {
      this.typeManager = typeManager;
      this.configuration = configuration;
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
      return null;
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
    private final List<AuthServiceExtension> extensions;

    public ApiServicesModule(
        TypeManager typeManager,
        JobStore jobStore,
        SymmetricKeyGenerator keyGenerator,
        List<AuthServiceExtension> extensions) {
      this.typeManager = typeManager;
      this.jobStore = jobStore;
      this.keyGenerator = keyGenerator;
      this.extensions = extensions;
    }

    @Override
    protected void configure() {
      MapBinder<String, AuthServiceExtension> mapBinder =
          MapBinder.newMapBinder(binder(), String.class, AuthServiceExtension.class);

      for (AuthServiceExtension provider : extensions) {
        mapBinder.addBinding(provider.getServiceId()).to(provider.getClass());
      }

      bind(AuthServiceProviderRegistry.class).to(PortabilityAuthServiceProviderRegistry.class);
      bind(SymmetricKeyGenerator.class).toInstance(keyGenerator);
      bind(TypeManager.class).toInstance(typeManager);
      bind(JobStore.class).toInstance(jobStore);
    }
  }
}
