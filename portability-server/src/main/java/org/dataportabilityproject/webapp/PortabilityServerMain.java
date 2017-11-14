package org.dataportabilityproject.webapp;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import org.dataportabilityproject.PortabilityFlags;
import org.dataportabilityproject.ServiceProviderRegistry;
import org.dataportabilityproject.cloud.CloudFactoryFactory;
import org.dataportabilityproject.cloud.interfaces.CloudFactory;
import org.dataportabilityproject.job.JobManager;
import org.dataportabilityproject.job.PortabilityJobFactory;
import org.dataportabilityproject.job.UUIDProvider;
import org.dataportabilityproject.shared.Secrets;

public class PortabilityServerMain {
  private static Secrets secrets;
  private static CloudFactory cloudFactory;
  private static ServiceProviderRegistry serviceProviderRegistry;
  private static PortabilityJobFactory portabilityJobFactory;
  private static JobManager jobManager;

  public static void main(String args[]) throws Exception {
    PortabilityFlags.parseArgs(args);

    // TODO: use Config.java from core library for the initialization stuff
    // Can probably make serviceProviderRegistry a singleton/factory class so that we don't
    // need to init here and pass along.
    secrets = new Secrets(PortabilityFlags.secretsFile());
    cloudFactory = CloudFactoryFactory.getCloudFactory(PortabilityFlags.cloud(), secrets);
    serviceProviderRegistry = new ServiceProviderRegistry(secrets, cloudFactory);
    jobManager = new JobManager(cloudFactory.getPersistentKeyValueStore());
    portabilityJobFactory = new PortabilityJobFactory(new UUIDProvider());

    // TODO: backlog and port should be command line args
    HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

    server.createContext("/_/listServices", new ListServicesHandler(serviceProviderRegistry));
    server.createContext("/_/listDataTypes", new ListDataTypesHandler(serviceProviderRegistry));
    server.createContext("/configure", new ConfigureHandler(serviceProviderRegistry,
        jobManager, portabilityJobFactory));

    server.setExecutor(Executors.newCachedThreadPool());
    server.start();

    System.out.println("Server is listening on port 8080" );
  }
}