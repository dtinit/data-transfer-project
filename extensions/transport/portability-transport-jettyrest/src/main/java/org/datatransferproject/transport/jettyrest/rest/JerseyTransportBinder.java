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
package org.datatransferproject.transport.jettyrest.rest;

import org.datatransferproject.api.action.Action;
import org.datatransferproject.spi.api.transport.TransportBinder;
import org.datatransferproject.transport.jettyrest.http.JettyTransport;
import org.datatransferproject.types.client.datatype.GetDataTypes;
import org.datatransferproject.types.client.transfer.*;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Binds {@link Action}s to REST over HTTP
 *
 * <p>Actions are bound by creating JAX-RS resources and registering them with the Jersey
 * application.
 */
public class JerseyTransportBinder implements TransportBinder {
  private final JettyTransport jettyTransport;
  private final Map<Class<?>, Action> actions;

  public JerseyTransportBinder(JettyTransport jettyTransport) {
    this.jettyTransport = jettyTransport;
    actions = new HashMap<>();
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> void bind(Action<T, ?> action) {
    actions.put(action.getRequestType(), action);
  }

  @SuppressWarnings("unchecked")
  public void start() {

    try {

      Set<Object> controllers = new HashSet<>();
      controllers.add(new DataTypesController(actions.get(GetDataTypes.class)));
      controllers.add(new TransferServicesController(actions.get(GetTransferServices.class)));
      controllers.add(
          new TransferController(
              actions.get(CreateTransferJob.class),
              actions.get(GenerateServiceAuthData.class),
              actions.get(ReserveWorker.class),
              actions.get(GetReservedWorker.class),
              actions.get(StartTransferJob.class),
              actions.get(GetTransferJob.class)));

      // Create a Jersey JAX-RS Application (resourceConfig), add the actions, and register it with
      // the Jetty transport.
      ResourceConfig resourceConfig = new ResourceConfig();

      // resourceConfig.register(JacksonFeature.class)
      resourceConfig.registerInstances(controllers);

      ServletContainer servletContainer = new ServletContainer(resourceConfig);
      jettyTransport.registerServlet("/api/*", servletContainer);

    } catch (Exception e) {
      throw new RuntimeException("Couldn't start JerseyTransportBinder", e);
    }
  }
}
