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
package org.datatransferproject.transport.jettyrest.http;

import org.eclipse.jetty.server.*;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.ErrorHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlet.Source;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.Writer;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;

/** Provides HTTP(s) communication to the system via Jetty. */
public class JettyTransport {
  private static final Logger logger = LoggerFactory.getLogger(JettyTransport.class);

  private static final String ANNOUNCE = "org.eclipse.jetty.util.log.announce";
  private static final String LOG_CLASS = "org.eclipse.jetty.util.log.class";

  private final KeyStore keyStore;
  private final boolean useHttps;

  private int httpPort = 8080; // TODO configure

  private Server server;
  private List<Handler> handlers = new ArrayList<>();

  public JettyTransport(KeyStore keyStore, boolean useHttps) {
    this.keyStore = keyStore;
    this.useHttps = useHttps;
    System.setProperty(LOG_CLASS, JettyMonitor.class.getName()); // required by Jetty
    System.setProperty(ANNOUNCE, "false");
    logger.info("Creating JettyTransport. useHttps=" + useHttps);
  }

  public void start() {
    if (useHttps) {
      server = new Server();
      SslContextFactory sslContextFactory = new SslContextFactory();
      sslContextFactory.setKeyStore(keyStore);
      // TODO configure
      sslContextFactory.setKeyStorePassword("password");
      sslContextFactory.setKeyManagerPassword("password");
      HttpConfiguration https = new HttpConfiguration();
      ServerConnector sslConnector =
              new ServerConnector(
                      server,
                      new SslConnectionFactory(sslContextFactory, "http/1.1"),
                      new HttpConnectionFactory(https));
      sslConnector.setPort(httpPort);
      server.setConnectors(new Connector[]{sslConnector});
    } else {
      server = new Server(httpPort);
      ServerConnector connector = new ServerConnector(server, new HttpConnectionFactory(new HttpConfiguration()));
      connector.setPort(httpPort);
      server.setConnectors(new Connector[]{connector});
    }

    server.setErrorHandler(new JettyErrorHandler());

    ContextHandlerCollection contexts = new ContextHandlerCollection();
    contexts.setHandlers(handlers.toArray(new Handler[0]));
    server.setHandler(contexts);
    try {
      server.start();
      logger.info("Using Jetty transport");
    } catch (Exception e) {
      throw new RuntimeException("Error starting Jetty transport", e);
    }
  }

  public void shutdown() {
    if (server == null) {
      return;
    }
    try {
      server.stop();
    } catch (Exception e) {
      throw new RuntimeException("Error stopping Jetty transport", e);
    }
  }

  public void registerServlet(String path, Servlet servletContainer) {
    ServletHolder servletHolder = new ServletHolder(Source.EMBEDDED);
    servletHolder.setName("Data Transfer Project");
    servletHolder.setServlet(servletContainer);
    servletHolder.setInitOrder(1);

    ServletContextHandler handler = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
    handler.setContextPath("/");

    handlers.add(handler);

    handler.getServletHandler().addServletWithMapping(servletHolder, path);
  }

  private class JettyErrorHandler extends ErrorHandler {
    protected void writeErrorPage(
        HttpServletRequest request, Writer writer, int code, String message, boolean showStacks)
        throws IOException {
      writer.write("{ error: '" + code + "'}");
    }
  }
}
