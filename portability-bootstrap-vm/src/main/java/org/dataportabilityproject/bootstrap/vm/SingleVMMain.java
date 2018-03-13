package org.dataportabilityproject.bootstrap.vm;

import com.google.common.util.concurrent.UncaughtExceptionHandlers;
import org.dataportabilityproject.gateway.ApiMain;
import org.dataportabilityproject.worker.WorkerMain;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Bootstraps all services (Gateway and 1..N Workers) in a single VM.
 *
 * <p>Intended for demonstration purposes.
 */
public class SingleVMMain {
  private final Consumer<Exception> errorCallback;
  private ExecutorService executorService;

  public static void main(String[] args) {
    Thread.setDefaultUncaughtExceptionHandler(UncaughtExceptionHandlers.systemExit());

    SingleVMMain singleVMMain = new SingleVMMain(SingleVMMain::exitError);

    Runtime.getRuntime().addShutdownHook(new Thread(singleVMMain::shutdown));

    // TODO make number of workers configurable
    singleVMMain.initializeWorkers(1);
    singleVMMain.initializeGateway();
  }

  public SingleVMMain(Consumer<Exception> errorCallback) {
    this.errorCallback = errorCallback;
  }

  public void initializeGateway() {
    ApiMain apiMain = new ApiMain();
    apiMain.initialize();
    try {
      apiMain.start();
    } catch (IOException e) {
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

  private static void exitError(Exception exception) {
    exception.printStackTrace();
    System.err.println("Exiting abnormally");
    System.exit(-1);
  }

  private class WorkerRunner implements Runnable {
    public void run() {
      //noinspection InfiniteLoopStatement
      while (true) {
        WorkerMain workerMain = new WorkerMain();
        try {
          workerMain.initialize();
        } catch (Exception e) {
          errorCallback.accept(e);
        }
        workerMain.poll();
      }
    }
  }
}
