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
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Writer;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

    /**
     * Hack to intercept any non-"api/*" requests that make it to the API server. This
     * includes:
     * - "/" -> index.html
     * - "/callback?token=foo" -> index.html?token=foo
     *
     * <p>This is NOT used in the demo-server since nginx handles these requests with
     * the Angular client.
     *
     * <p>This IS used by the demo-google-deployment, as the Google Cloud Load Balancer
     * currently used in that deployment can't do URL remaps - so we solve that by setting
     * our default mapping to the API backends and redirecting those requests here.
     * Otherwise "/" will map to our static bucket and display a directory listing rather
     * than index.html.
     *
     * TODO(rtannenbaum): Move this somewhere better than the error handler
     */
    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
            throws IOException
    {
      logger.warn("handling non-'api/*' request: " + request.getServletPath()
              + ". Do NOT do this in production environments!");
      if (request.getServletPath().equals("/")) {
        String redirect = request.getServerName() + "/index.html";
        redirect = (request.isSecure() || !request.getServerName().equals("localhost") ? "https://" : "http://")
                + redirect;
        logger.info("redirecting to: " + redirect);
        response.sendRedirect(redirect);
      } else if (request.getServletPath().startsWith("/callback")) {
        // This is a hack to redirect OAuth callbacks back to the frontend so creds may be processed there.
        // This should only be done for demo purposes. Secure implementations may not want to allow creds
        // to reach here (i.e. the API server). We do this here due to limitations with the load balancer
        // used in demo-google-deployment (we cannot handle the redirect there with the Google Cloud Load
        // Balancer currently).
        String redirect = request.getServerName() + "/index.html?";
        redirect = (request.isSecure() || !request.getServerName().equals("localhost") ? "https://" : "http://")
                + redirect;
        for (Map.Entry<String, String[]> header : request.getParameterMap().entrySet()) {
          if (header.getKey().equals("code")
              || header.getKey().equals("frob")
              || header.getKey().equals("oauth_verifier")) {
            redirect += header.getKey() + "=" + header.getValue()[0];
          }
        }
        logger.info("redirecting to: " + redirect);
        response.sendRedirect(redirect);
      } else {
        doError(target, baseRequest, request, response);
      }
    }

    protected void writeErrorPage(
        HttpServletRequest request, Writer writer, int code, String message, boolean showStacks)
        throws IOException {
      writer.write("{ error: '" + code + "'}");
    }
  }
}
