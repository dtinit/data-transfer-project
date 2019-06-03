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
package org.datatransferproject.api;

import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.util.Types;
import org.datatransferproject.api.action.Action;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.api.launcher.TypeManager;
import org.datatransferproject.api.token.JWTTokenManager;
import org.datatransferproject.config.extension.SettingsExtension;
import org.datatransferproject.launcher.types.TypeManagerImpl;
import org.datatransferproject.security.AesSymmetricKeyGenerator;
import org.datatransferproject.security.SymmetricKeyGenerator;
import org.datatransferproject.spi.api.auth.extension.AuthServiceExtension;
import org.datatransferproject.spi.api.token.TokenManager;
import org.datatransferproject.spi.api.transport.TransportBinder;
import org.datatransferproject.spi.cloud.extension.CloudExtension;
import org.datatransferproject.spi.cloud.storage.AppCredentialStore;
import org.datatransferproject.spi.cloud.storage.JobStore;
import org.datatransferproject.spi.cloud.storage.TemporaryPerJobDataStore;
import org.datatransferproject.spi.service.extension.ServiceExtension;
import org.datatransferproject.types.transfer.auth.TokenAuthData;
import org.datatransferproject.types.transfer.auth.TokenSecretAuthData;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.IOException;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;

import static org.datatransferproject.config.extension.SettingsExtensionLoader.getSettingsExtension;
import static org.datatransferproject.launcher.monitor.MonitorLoader.loadMonitor;
import static org.datatransferproject.spi.cloud.extension.CloudExtensionLoader.getCloudExtension;

/** Starts the api server. */
public class ApiMain {

  private final Monitor monitor;
  private List<ServiceExtension> serviceExtensions = Collections.emptyList();

  /** Starts the api server, currently the reference implementation. */
  public static void main(String[] args) {

    Monitor monitor = loadMonitor();
    monitor.info(() -> "Starting API Server.");

    Thread.setDefaultUncaughtExceptionHandler(
        (thread, t) ->
            monitor.severe(() -> "Uncaught exception in thread: " + thread.getName(), t));

    ApiMain apiMain = new ApiMain(monitor);
    apiMain.initializeHttp();
    apiMain.start();
  }

  public ApiMain(Monitor monitor) {
    this.monitor = monitor;
  }

  public void initializeHttp() {
    initializeHttps(null, null, null);
  }

  public void initializeHttps(
      TrustManagerFactory trustManagerFactory,
      KeyManagerFactory keyManagerFactory,
      KeyStore keyStore) {

    // TODO init with types
    TypeManager typeManager = new TypeManagerImpl();
    typeManager.registerTypes(
        TokenAuthData.class, TokensAndUrlAuthData.class, TokenSecretAuthData.class);

    SettingsExtension settingsExtension = getSettingsExtension();

    settingsExtension.initialize();
    ApiExtensionContext extensionContext =
        new ApiExtensionContext(typeManager, settingsExtension, monitor);

    if (trustManagerFactory != null) {
      extensionContext.registerService(TrustManagerFactory.class, trustManagerFactory);
    }

    if (keyManagerFactory != null) {
      extensionContext.registerService(KeyManagerFactory.class, keyManagerFactory);
    }

    if (keyStore != null) {
      extensionContext.registerService(KeyStore.class, keyStore);
    }

    extensionContext.registerService(HttpTransport.class, new NetHttpTransport());
    extensionContext.registerService(JsonFactory.class, new JacksonFactory());

    // Services that need to be shared between authServiceExtensions or load types in the
    // typemanager get initialized first.
    serviceExtensions = new ArrayList<>();
    ServiceLoader.load(ServiceExtension.class).iterator().forEachRemaining(serviceExtensions::add);

    serviceExtensions.forEach((se) -> se.initialize(extensionContext));

    CloudExtension cloudExtension = getCloudExtension();
    cloudExtension.initialize(extensionContext);

    // Needed for GoogleAuthServiceExtension
    extensionContext.registerService(HttpTransport.class, new NetHttpTransport());
    extensionContext.registerService(JobStore.class, cloudExtension.getJobStore());
    extensionContext.registerService(TemporaryPerJobDataStore.class, cloudExtension.getJobStore());
    extensionContext.registerService(
        AppCredentialStore.class, cloudExtension.getAppCredentialStore());

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
    SymmetricKeyGenerator keyGenerator = new AesSymmetricKeyGenerator(monitor);
    TokenManager tokenManager;

    try {
      // TODO: we store the JWT Token with the application credentials, but dont need to have a key
      // consider using a blobstore type of thing or allowing the AppCredentialStore to return a
      // cred that doesn't contain a key.
      tokenManager =
          new JWTTokenManager(
              cloudExtension
                  .getAppCredentialStore()
                  .getAppCredentials(JWTTokenManager.JWT_KEY_NAME, JWTTokenManager.JWT_SECRET_NAME)
                  .getSecret(),
              monitor);
    } catch (IOException e) {
      monitor.info(
          () -> "Unable to initialize JWTTokenManager, did you specify a JWT_KEY and JWT_SECRET?",
          e);
      throw new RuntimeException(e);
    }

    Injector injector;
    try {
       injector =
          Guice.createInjector(
              new ApiServicesModule(
                  typeManager,
                  cloudExtension.getJobStore(),
                  keyGenerator,
                  trustManagerFactory,
                  keyManagerFactory,
                  authServiceExtensions,
                  tokenManager,
                  extensionContext));

    } catch (Exception e) {
      monitor.info(
          () -> "Error initializing Guice",
          e);
      throw e;
    }

    extensionContext.registerService(Injector.class, injector);

    bindActions(injector, extensionContext);
  }

  public void start() {
    serviceExtensions.forEach(ServiceExtension::start);
  }

  public void stop() {
    serviceExtensions.forEach(ServiceExtension::shutdown);
  }

  private void bindActions(Injector injector, ApiExtensionContext context) {
    TransportBinder binder = context.getService(TransportBinder.class);
    if (binder == null) {
      return;
    }
    TypeLiteral<Set<Action>> literal = setOf(Action.class);
    Key<Set<Action>> key = Key.get(literal);
    Set<Action> actions = injector.getInstance(key);
    actions.forEach(binder::bind);
  }

  @SuppressWarnings("unchecked")
  public static final <T> TypeLiteral<Set<T>> setOf(Class<T> type) {
    return (TypeLiteral<Set<T>>) TypeLiteral.get(Types.setOf(type));
  }
}
