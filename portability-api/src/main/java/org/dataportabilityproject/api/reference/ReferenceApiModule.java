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
package org.dataportabilityproject.api.reference;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Provides;
import com.google.inject.multibindings.MapBinder;
import com.sun.net.httpserver.HttpHandler;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import javax.inject.Named;
import org.dataportabilityproject.api.launcher.ExtensionContext;
import org.dataportabilityproject.config.FlagBindingModule;
import org.dataportabilityproject.security.AsymmetricKeyGenerator;
import org.dataportabilityproject.security.RsaSymmetricKeyGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bindings for the reference api server, a sample implementation using Sun's http library to serve
 * requests for the api actions.
 */
public class ReferenceApiModule extends FlagBindingModule {
  private static final Logger logger = LoggerFactory.getLogger(ReferenceApiModule.class);

  private final ExtensionContext context;

  public ReferenceApiModule(ExtensionContext context) {
    this.context = context;
  }

  @Override
  protected void configure() {
    // binds flags from ExtensionContext to @Named annotations
    bindFlags(context);
    // TODO: requireBinding for Strings annotated with @Named("baseUrl") and @Named("baseApiUrl")

    // TODO: Bind actions in single or multiple modules
    MapBinder<String, HttpHandler> mapbinder =
        MapBinder.newMapBinder(binder(), String.class, HttpHandler.class);

    // HttpServer does exact longest matching prefix for context matching. This means
    // /_/listServices, /_/listServicesthisshouldnotwork and /_/listServices/path/to/resource will
    // all be handled by the ListServicesHandler below. To prevent this, each handler below should
    // validate the request URI that it is getting passed in.
    mapbinder.addBinding(DataTransferHandler.PATH).to(DataTransferHandler.class);
    mapbinder.addBinding(ListDataTypesHandler.PATH).to(ListDataTypesHandler.class);
    mapbinder.addBinding(ListServicesHandler.PATH).to(ListServicesHandler.class);
    mapbinder.addBinding(LegacyAuthCallbackHandler.PATH).to(LegacyAuthCallbackHandler.class);
    mapbinder.addBinding(OauthCallbackHandler.PATH).to(OauthCallbackHandler.class);
    mapbinder.addBinding(Oauth2CallbackHandler.PATH).to(Oauth2CallbackHandler.class);
    mapbinder.addBinding(SimpleLoginSubmitHandler.PATH).to(SimpleLoginSubmitHandler.class);
    mapbinder.addBinding(CopySetupHandler.PATH).to(CopySetupHandler.class);
    mapbinder.addBinding(ImportSetupHandler.PATH).to(ImportSetupHandler.class);
    mapbinder.addBinding(StartCopyHandler.PATH).to(StartCopyHandler.class);

    bind(AsymmetricKeyGenerator.class).to(RsaSymmetricKeyGenerator.class);
  }

  @Provides
  @Named("httpPort")
  public int getHttpPort() {
    return 8080; // TODO: set with a flag
  }

  @Provides
  @Named("defaultView")
  public String getDefaultView() {
    return "static/index.html"; // TODO: set with a flag
  }

  @Provides
  @Named("httpExecutor")
  public Executor getExecutor(UncaughtExceptionHandler uncaughtExceptionHandler) {
    ThreadFactory threadPoolFactory =
        new ThreadFactoryBuilder()
            .setNameFormat("http-server-%d")
            .setUncaughtExceptionHandler(uncaughtExceptionHandler)
            .build();
    return Executors.newCachedThreadPool(threadPoolFactory);
  }

  @Provides
  public UncaughtExceptionHandler uncaughtExceptionHandler() {
    return new UncaughtExceptionHandler() {
      @Override
      public void uncaughtException(Thread thread, Throwable t) {
        logger.warn("Uncaught exception in thread: {}", thread.getName(), t);
      }
    };
  }
}
