/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.dataportabilityproject.webapp;

import static org.dataportabilityproject.webapp.SetupHandler.Mode.COPY;
import static org.dataportabilityproject.webapp.SetupHandler.Mode.IMPORT;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import org.dataportabilityproject.PortabilityFlags;
import org.dataportabilityproject.ServiceProviderRegistry;
import org.dataportabilityproject.cloud.CloudFactoryFactory;
import org.dataportabilityproject.cloud.interfaces.CloudFactory;
import org.dataportabilityproject.job.Crypter;
import org.dataportabilityproject.job.JobDao;
import org.dataportabilityproject.job.PortabilityJobFactory;
import org.dataportabilityproject.job.UUIDProvider;

public class PortabilityApiMain {

  private static CloudFactory cloudFactory;
  private static ServiceProviderRegistry serviceProviderRegistry;
  private static PortabilityJobFactory portabilityJobFactory;
  private static CryptoHelper cryptoHelper;
  private static JobDao jobDao;

  public static void main(String args[]) throws Exception {
    PortabilityFlags.parse();
    PortabilityApiFlags.parse();

    // TODO: use Config.java from core library for the initialization stuff
    // Can probably make serviceProviderRegistry a singleton/factory class so that we don't
    // need to initFlagsAndSecrets here and pass along.
    cloudFactory = CloudFactoryFactory.getCloudFactory(PortabilityFlags.cloud());
    serviceProviderRegistry = new ServiceProviderRegistry(
        cloudFactory, PortabilityFlags.supportedServiceProviders());
    jobDao = new JobDao(cloudFactory.getPersistentKeyValueStore());
    portabilityJobFactory = new PortabilityJobFactory(new UUIDProvider());
    // TODO: Wire up the correct Crypter.
    cryptoHelper = new CryptoHelper(jobDao);

    // TODO: backlog and port should be command line args
    HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

    // HttpServer does exact longest matching prefix for context matching. This means
    // /_/listServices, /_/listServicesthisshouldnotwork and /_/listServices/path/to/resource will
    // all be handled by the ListServicesHandler below. To prevent this, each handler below should
    // validate the request URI that it is getting passed in.
    server.createContext("/_/copySetup",
        new SetupHandler(serviceProviderRegistry, jobDao, COPY, "/_/copySetup"));
    server.createContext("/_/importSetup",
        new SetupHandler(serviceProviderRegistry, jobDao, IMPORT, "/_/importSetup"));
    server.createContext("/_/listDataTypes", new ListDataTypesHandler(serviceProviderRegistry));
    server.createContext("/_/listServices", new ListServicesHandler(serviceProviderRegistry));
    server.createContext("/_/startCopy",
        new StartCopyHandler(serviceProviderRegistry, jobDao, cloudFactory, cryptoHelper));
    server.createContext("/callback/",
        new Oauth2CallbackHandler(serviceProviderRegistry, jobDao, cryptoHelper));
    server.createContext("/callback1/",
        new OauthCallbackHandler(serviceProviderRegistry, jobDao, cryptoHelper));
    server.createContext("/configure", new ConfigureHandler(serviceProviderRegistry,
        jobDao, portabilityJobFactory));
    server.createContext("/simpleLoginSubmit",
        new SimpleLoginSubmitHandler(serviceProviderRegistry, jobDao, cryptoHelper));

    // Redirect anything that doesn't match to the ViewHandler. The view handler serves index.html
    // which should reference static content served by our bucket. The angular app then routes requests
    // client side via angular.
    server.createContext("/", new ViewHandler());

    server.setExecutor(Executors.newCachedThreadPool());
    server.start();

    System.out.println("Server is listening on port 8080");
  }
}