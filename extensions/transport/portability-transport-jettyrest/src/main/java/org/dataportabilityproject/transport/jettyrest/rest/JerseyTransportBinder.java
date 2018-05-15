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
package org.dataportabilityproject.transport.jettyrest.rest;

import org.dataportabilityproject.api.action.Action;
import org.dataportabilityproject.spi.api.transport.TransportBinder;
import org.dataportabilityproject.transport.jettyrest.http.JettyTransport;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;

/**
 * Binds {@link Action}s to REST over HTTP
 *
 * <p>Actions are bound by creating JAX-RS resources and registering them with the Jersey
 * application.
 */
public class JerseyTransportBinder implements TransportBinder {
  private JettyTransport jettyTransport;

  public JerseyTransportBinder(JettyTransport jettyTransport) {
    this.jettyTransport = jettyTransport;
  }

  @Override
  public void bind(Action action) {}

  public void start() {
    // Create a Jersey JAX-RS Application (resourceConfig), add the actions, and register it with
    // the Jetty transport.
    ResourceConfig resourceConfig = new ResourceConfig();

    // resourceConfig.register(JacksonFeature.class)
    // resourceConfig.registerInstances();

    ServletContainer servletContainer = new ServletContainer(resourceConfig);
    jettyTransport.registerServlet("/api/*", servletContainer);
  }
}
