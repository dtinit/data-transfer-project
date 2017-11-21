package org.dataportabilityproject.webapp;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import org.dataportabilityproject.PortabilityFlags;
import org.dataportabilityproject.ServiceProviderRegistry;
import org.dataportabilityproject.cloud.CloudFactoryFactory;
import org.dataportabilityproject.cloud.interfaces.CloudFactory;
import org.dataportabilityproject.job.Crypter;
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
  private static CryptoHelper cryptoHelper;

  public static void main(String args[]) throws Exception {
    PortabilityFlags.parseArgs(args);

    // TODO: use Config.java from core library for the initialization stuff
    // Can probably make serviceProviderRegistry a singleton/factory class so that we don't
    // need to init here and pass along.
    secrets = new Secrets("secrets.csv");
    cloudFactory = CloudFactoryFactory.getCloudFactory(PortabilityFlags.cloud(), secrets);
    serviceProviderRegistry = new ServiceProviderRegistry(secrets, cloudFactory);
    jobManager = new JobManager(cloudFactory.getPersistentKeyValueStore());
    portabilityJobFactory = new PortabilityJobFactory(new UUIDProvider());
    // TODO: Wire up the correct Crypter.
    cryptoHelper = new CryptoHelper(new Crypter() {
    });

    // TODO: backlog and port should be command line args
    HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

    // HttpServer does exact longest matching prefix for context matching. This means
    // /_/listServices, /_/listServicesthisshouldnotwork and /_/listServices/path/to/resource will
    // all be handled by the ListServicesHandler below. To prevent this, each handler below should
    // validate the request URI that it is getting passed in.
    server.createContext("/_/copySetup", new CopySetupHandler(serviceProviderRegistry, jobManager));
    server.createContext("/_/importSetup",
        new ImportSetupHandler(serviceProviderRegistry, jobManager));
    server.createContext("/_/listDataTypes", new ListDataTypesHandler(serviceProviderRegistry));
    server.createContext("/_/listServices", new ListServicesHandler(serviceProviderRegistry));
    server.createContext("/_/startCopy",
        new StartCopyHandler(serviceProviderRegistry, jobManager, cloudFactory));
    server.createContext("/callback/",
        new Oauth2CallbackHandler(serviceProviderRegistry, jobManager, cryptoHelper));
    server.createContext("/configure", new ConfigureHandler(serviceProviderRegistry,
        jobManager, portabilityJobFactory));

    server.setExecutor(Executors.newCachedThreadPool());
    server.start();

    System.out.println("Server is listening on port 8080");
  }
}