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
package org.datatransferproject.transfer;

import static org.datatransferproject.config.extension.SettingsExtensionLoader.getSettingsExtension;
import static org.datatransferproject.launcher.monitor.MonitorLoader.loadMonitor;
import static org.datatransferproject.spi.cloud.extension.CloudExtensionLoader.getCloudExtension;
import static org.datatransferproject.spi.transfer.hooks.JobHooksLoader.loadJobHooks;

import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.UncaughtExceptionHandlers;
import com.google.inject.Guice;
import com.google.inject.Injector;
import java.util.List;
import java.util.ServiceLoader;
import okhttp3.OkHttpClient;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.config.extension.SettingsExtension;
import org.datatransferproject.security.AesSymmetricKeyGenerator;
import org.datatransferproject.security.SymmetricKeyGenerator;
import org.datatransferproject.spi.cloud.extension.CloudExtension;
import org.datatransferproject.spi.cloud.storage.AppCredentialStore;
import org.datatransferproject.spi.cloud.storage.JobStore;
import org.datatransferproject.spi.cloud.storage.TemporaryPerJobDataStore;
import org.datatransferproject.spi.service.extension.ServiceExtension;
import org.datatransferproject.spi.transfer.extension.TransferExtension;
import org.datatransferproject.spi.transfer.hooks.JobHooks;
import org.datatransferproject.spi.transfer.idempotentexecutor.IdempotentImportExecutor;
import org.datatransferproject.spi.transfer.idempotentexecutor.IdempotentImportExecutorLoader;
import org.datatransferproject.spi.transfer.security.SecurityExtension;
import org.datatransferproject.spi.transfer.security.SecurityExtensionLoader;

/**
 * Main class to bootstrap a portability transfer worker that will operate on a single job whose
 * state is held in {@link JobMetadata}.
 */
public class WorkerMain {

  private Worker worker;

  public static void main(String[] args) {
    Thread.setDefaultUncaughtExceptionHandler(UncaughtExceptionHandlers.systemExit());

    WorkerMain workerMain = new WorkerMain();
    workerMain.initialize();
    workerMain.poll();

    System.exit(0);
  }

  public void initialize() {
    Monitor monitor = loadMonitor();

    SettingsExtension settingsExtension = getSettingsExtension();
    settingsExtension.initialize();
    WorkerExtensionContext extensionContext =
        new WorkerExtensionContext(settingsExtension, monitor);

    // TODO this should be moved into a service extension
    extensionContext.registerService(HttpTransport.class, new NetHttpTransport());
    extensionContext.registerService(OkHttpClient.class, new OkHttpClient.Builder().build());
    extensionContext.registerService(JsonFactory.class, new JacksonFactory());

    ServiceLoader.load(ServiceExtension.class)
        .iterator()
        .forEachRemaining(serviceExtension -> serviceExtension.initialize(extensionContext));

    // TODO: verify that this is the cloud extension that is specified in the configuration
    CloudExtension cloudExtension = getCloudExtension();
    cloudExtension.initialize(extensionContext);
    monitor.info(() -> "Using CloudExtension: " + cloudExtension.getClass().getName());

    JobStore jobStore = cloudExtension.getJobStore();
    extensionContext.registerService(JobStore.class, jobStore);
    extensionContext.registerService(TemporaryPerJobDataStore.class, jobStore);

    AppCredentialStore appCredentialStore = cloudExtension.getAppCredentialStore();
    extensionContext.registerService(AppCredentialStore.class, appCredentialStore);

    List<TransferExtension> transferExtensions = getTransferExtensions(monitor);

    // Load security extension and services
    SecurityExtension securityExtension =
        SecurityExtensionLoader.getSecurityExtension(extensionContext);
    monitor.info(() -> "Using SecurityExtension: " + securityExtension.getClass().getName());

    IdempotentImportExecutor idempotentImportExecutor =
        IdempotentImportExecutorLoader.load(extensionContext);
    monitor.info(
        () -> "Using IdempotentImportExecutor: " + idempotentImportExecutor.getClass().getName());

    // TODO: make configurable
    SymmetricKeyGenerator symmetricKeyGenerator = new AesSymmetricKeyGenerator(monitor);

    JobHooks jobHooks = loadJobHooks();

    Injector injector = null;
    try {
      injector =
          Guice.createInjector(
              new WorkerModule(
                  extensionContext,
                  cloudExtension,
                  transferExtensions,
                  securityExtension,
                  idempotentImportExecutor,
                  symmetricKeyGenerator,
                  jobHooks));
    } catch (Exception e) {
      monitor.severe(() -> "Unable to initialize Guice in Worker", e);
      throw e;
    }
    worker = injector.getInstance(Worker.class);

    // Reset the JobMetadata in case set previously when running SingleVMMain
    JobMetadata.reset();
  }

  public void poll() {
    worker.doWork();
  }

  private static List<TransferExtension> getTransferExtensions(Monitor monitor) {
    // TODO: Next version should ideally not load every TransferExtension impl, look into
    // solutions where we selectively invoke class loader.
    ImmutableList.Builder<TransferExtension> extensionsBuilder = ImmutableList.builder();
    // Note that initialization of the TransferExtension is done in the WorkerModule since they're
    // initialized as they're requested.
    ServiceLoader.load(TransferExtension.class)
        .iterator()
        .forEachRemaining(
            ext -> {
              monitor.info(
                  () -> "Loading transfer extension: " + ext + " for " + ext.getServiceId());
              extensionsBuilder.add(ext);
            });
    ImmutableList<TransferExtension> extensions = extensionsBuilder.build();
    Preconditions.checkState(
        !extensions.isEmpty(), "Could not find any implementations of TransferExtension");
    return extensions;
  }
}
