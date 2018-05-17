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
package org.dataportabilityproject.api;

import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.util.Types;
import org.dataportabilityproject.api.action.Action;
import org.dataportabilityproject.api.launcher.TypeManager;
import org.dataportabilityproject.api.token.JWTTokenManager;
import org.dataportabilityproject.config.extension.SettingsExtension;
import org.dataportabilityproject.launcher.impl.TypeManagerImpl;
import org.dataportabilityproject.security.AesSymmetricKeyGenerator;
import org.dataportabilityproject.security.SymmetricKeyGenerator;
import org.dataportabilityproject.spi.api.auth.extension.AuthServiceExtension;
import org.dataportabilityproject.spi.api.token.TokenManager;
import org.dataportabilityproject.spi.api.transport.TransportBinder;
import org.dataportabilityproject.spi.cloud.extension.CloudExtension;
import org.dataportabilityproject.spi.cloud.storage.AppCredentialStore;
import org.dataportabilityproject.spi.cloud.storage.JobStore;
import org.dataportabilityproject.spi.service.extension.ServiceExtension;
import org.dataportabilityproject.types.transfer.auth.TokenAuthData;
import org.dataportabilityproject.types.transfer.auth.TokenSecretAuthData;
import org.dataportabilityproject.types.transfer.auth.TokensAndUrlAuthData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;

/** Starts the api server. */
public class ApiMain {
  private static final Logger logger = LoggerFactory.getLogger(ApiMain.class);

  private List<ServiceExtension> serviceExtensions = Collections.emptyList();

  /** Starts the api server, currently the reference implementation. */
  public static void main(String[] args) {
    logger.warn("Starting reference api server.");
    Thread.setDefaultUncaughtExceptionHandler(
        (thread, t) -> logger.warn("Uncaught exception in thread: {}", thread.getName(), t));

    ApiMain apiMain = new ApiMain();
    apiMain.initializeHttp();
    apiMain.start();
  }

  public void initializeHttp() {
    initializeHttps(null, null);
  }

  public void initializeHttps(
      TrustManagerFactory trustManagerFactory, KeyManagerFactory keyManagerFactory) {
    // TODO init with types
    TypeManager typeManager = new TypeManagerImpl();
    typeManager.registerTypes(
        TokenAuthData.class, TokensAndUrlAuthData.class, TokenSecretAuthData.class);

    SettingsExtension settingsExtension = getSettingsExtension();

    settingsExtension.initialize(null);
    ApiExtensionContext extensionContext = new ApiExtensionContext(typeManager, settingsExtension);

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
    SymmetricKeyGenerator keyGenerator = new AesSymmetricKeyGenerator();
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
                  .getSecret());
    } catch (IOException e) {
      logger.error(
          "Unable to initialize JWTTokenManager, did you specify a JWT_KEY and JWT_SECRET?");
      throw new RuntimeException(e);
    }

    Injector injector =
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

    extensionContext.registerService(Injector.class, injector);

    bindActions(injector, extensionContext);
  }

  public void start() {
    serviceExtensions.forEach(ServiceExtension::start);
  }

  public void stop() {
    serviceExtensions.forEach(ServiceExtension::shutdown);
  }

  private SettingsExtension getSettingsExtension() {
    ImmutableList.Builder<SettingsExtension> extensionsBuilder = ImmutableList.builder();
    ServiceLoader.load(SettingsExtension.class).iterator().forEachRemaining(extensionsBuilder::add);
    ImmutableList<SettingsExtension> extensions = extensionsBuilder.build();
    Preconditions.checkState(
        extensions.size() == 1,
        "Exactly one SettingsExtension is required, but found " + extensions.size());
    return extensions.get(0);
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
  public static <T> TypeLiteral<Set<T>> setOf(Class<T> type) {
    return (TypeLiteral<Set<T>>) TypeLiteral.get(Types.setOf(type));
  }
}
