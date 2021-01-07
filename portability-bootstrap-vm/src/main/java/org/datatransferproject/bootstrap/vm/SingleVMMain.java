package org.datatransferproject.bootstrap.vm;

import static java.lang.String.format;
import static org.datatransferproject.launcher.monitor.MonitorLoader.loadMonitor;

import com.google.common.util.concurrent.UncaughtExceptionHandlers;
import java.io.InputStream;
import java.security.KeyStore;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import org.datatransferproject.api.ApiMain;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.api.launcher.Version;
import org.datatransferproject.transfer.WorkerMain;

/**
 * Bootstraps all services (Gateway and 1..N Workers) in a single VM.
 *
 * <p>Intended for demonstration purposes.
 */
public class SingleVMMain {
  private final Consumer<Exception> errorCallback;
  private final Monitor monitor;
  private ExecutorService executorService;

  public static void main(String[] args) {
    Thread.setDefaultUncaughtExceptionHandler(UncaughtExceptionHandlers.systemExit());

    SingleVMMain singleVMMain = new SingleVMMain();

    Runtime.getRuntime().addShutdownHook(new Thread(singleVMMain::shutdown));

    // TODO make number of workers configurable
    singleVMMain.initializeWorkers(1);
    singleVMMain.initializeGateway();
  }

  public SingleVMMain() {
    this.monitor = loadMonitor();
    this.errorCallback =
        (e) -> {
          monitor.severe(() -> "Exiting abnormally", e);
          monitor.flushLogs();
          System.exit(-1);
        };
  }

  public void initializeGateway() {
    String version = Version.getVersion();
    String hash = Version.getSourceHash();

    monitor.info(() -> format("Running version %s. Source repository hash %s", version, hash));

    ApiMain apiMain = new ApiMain(monitor);

    try (InputStream stream =
        ApiMain.class.getClassLoader().getResourceAsStream("demo-selfsigned-keystore.jks")) {
      if (stream == null) {
        throw new IllegalArgumentException("Demo keystore was not found");
      }

      // initialise the keystore
      char[] password = "password".toCharArray();
      KeyStore keyStore = KeyStore.getInstance("JKS");
      keyStore.load(stream, password);
      KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
      keyManagerFactory.init(keyStore, password);

      TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("SunX509");
      trustManagerFactory.init(keyStore);

      apiMain.initializeHttps(trustManagerFactory, keyManagerFactory, keyStore);

      apiMain.start();

    } catch (Exception e) {
      errorCallback.accept(e);
    }
  }

  public void initializeWorkers(int workers) {
    if (workers < 1) {
      errorCallback.accept(new IllegalArgumentException("Invalid number of workers: " + workers));
      return;
    }

    executorService = Executors.newFixedThreadPool(workers);

    for (int i = 0; i < workers; i++) {
      WorkerRunner workerRunner = new WorkerRunner();
      executorService.submit(workerRunner);
    }
  }

  public void shutdown() {
    if (executorService != null) {
      executorService.shutdownNow();
    }
  }

  private class WorkerRunner implements Runnable {
    public void run() {
      //noinspection InfiniteLoopStatement
      while (true) {
        try {
          WorkerMain workerMain = new WorkerMain();
          workerMain.initialize();
          workerMain.poll();
        } catch (Exception e) {
          errorCallback.accept(e);
        }
      }
    }
  }
}
