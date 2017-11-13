package org.dataportabilityproject.webapp;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import org.dataportabilityproject.ServiceProviderRegistry;
import org.dataportabilityproject.cloud.CloudFactoryFactory;
import org.dataportabilityproject.cloud.SupportedCloud;
import org.dataportabilityproject.cloud.interfaces.CloudFactory;
import org.dataportabilityproject.cloud.local.LocalCloudFactory;
import org.dataportabilityproject.shared.Secrets;


public class PortabilityServerMain {
  private static Secrets secrets;
  private static CloudFactory cloudFactory;
  private static ServiceProviderRegistry serviceProviderRegistry;

  public static void main(String args[]) throws Exception {
    // TODO: use Config.java from core library for the initialization stuff
    // Can probably make serviceProviderRegistry a singleton/factory class so that we don't
    // need to init here and pass along.
    secrets = new Secrets("secrets.csv");
    cloudFactory = CloudFactoryFactory.getCloudFactory(SupportedCloud.LOCAL, secrets);
    serviceProviderRegistry = new ServiceProviderRegistry(secrets, cloudFactory);


    InetSocketAddress addr = new InetSocketAddress(8080);
    HttpServer server = HttpServer.create(addr, 0);
    server.createContext("/hello", new HelloWorldHandler());
    server.createContext("/_/listServices", new ListServicesHandler(serviceProviderRegistry));
    server.createContext("/_/listDataTypes", new ListDataTypesHandler(serviceProviderRegistry));

    server.setExecutor(Executors.newCachedThreadPool());
    server.start();
    System.out.println("Server is listening on port 8080" );
  }

}
