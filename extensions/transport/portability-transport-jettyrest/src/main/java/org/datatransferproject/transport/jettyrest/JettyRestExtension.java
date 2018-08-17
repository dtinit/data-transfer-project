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
package org.datatransferproject.transport.jettyrest;

import org.datatransferproject.api.launcher.ExtensionContext;
import org.datatransferproject.spi.api.transport.TransportBinder;
import org.datatransferproject.spi.service.extension.ServiceExtension;
import org.datatransferproject.transport.jettyrest.http.JettyTransport;
import org.datatransferproject.transport.jettyrest.rest.JerseyTransportBinder;

import java.security.KeyStore;

/**
 * Bootstraps the Jetty REST extension.
 *
 * <p>Binds {@link org.datatransferproject.api.action.Action}s to REST over HTTP using Jetty and
 * Jersey.
 */
public class JettyRestExtension implements ServiceExtension {
  private JettyTransport transport;
  private JerseyTransportBinder binder;

  @Override
  public void initialize(ExtensionContext context) {
    KeyStore keyStore = context.getService(KeyStore.class);
    boolean useHttps = context.getSetting("useHttps", true);
    transport = new JettyTransport(keyStore, useHttps);
    binder = new JerseyTransportBinder(transport);
    context.registerService(TransportBinder.class, binder);
  }

  @Override
  public void start() {
    binder.start();
    transport.start();
  }

  @Override
  public void shutdown() {
    transport.shutdown();
  }
}
