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

import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.UncaughtExceptionHandlers;
import com.google.inject.Guice;
import com.google.inject.Injector;
import okhttp3.OkHttpClient;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.config.extension.SettingsExtension;
import org.datatransferproject.security.AesSymmetricKeyGenerator;
import org.datatransferproject.security.AsymmetricKeyGenerator;
import org.datatransferproject.security.RsaSymmetricKeyGenerator;
import org.datatransferproject.security.SymmetricKeyGenerator;
import org.datatransferproject.spi.cloud.extension.CloudExtension;
import org.datatransferproject.spi.cloud.storage.AppCredentialStore;
import org.datatransferproject.spi.cloud.storage.JobStore;
import org.datatransferproject.spi.service.extension.ServiceExtension;
import org.datatransferproject.spi.transfer.extension.TransferExtension;
import org.datatransferproject.spi.transfer.hooks.JobHooks;
import org.datatransferproject.spi.transfer.security.AuthDataDecryptService;
import org.datatransferproject.spi.transfer.security.PublicKeySerializer;
import org.datatransferproject.spi.transfer.security.SecurityExtension;

import java.util.HashSet;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;

import static java.util.stream.Collectors.toSet;
import static org.datatransferproject.config.extension.SettingsExtensionLoader.getSettingsExtension;
import static org.datatransferproject.launcher.monitor.MonitorLoader.loadMonitor;
import static org.datatransferproject.spi.cloud.extension.CloudExtensionLoader.getCloudExtension;
import static org.datatransferproject.spi.transfer.hooks.JobHooksLoader.loadJobHooks;

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

    AppCredentialStore appCredentialStore = cloudExtension.getAppCredentialStore();
    extensionContext.registerService(AppCredentialStore.class, appCredentialStore);

    List<TransferExtension> transferExtensions = getTransferExtensions(monitor);

    Set<SecurityExtension> securityExtensions = new HashSet<>();
    ServiceLoader.load(SecurityExtension.class)
        .iterator()
        .forEachRemaining(securityExtensions::add);
    securityExtensions.forEach(e -> e.initialize(extensionContext));

    Set<PublicKeySerializer> publicKeySerializers =
        securityExtensions
            .stream()
            .flatMap(e -> e.getPublicKeySerializers().stream())
            .collect(toSet());

    Set<AuthDataDecryptService> decryptServices =
        securityExtensions.stream().flatMap(e -> e.getDecryptServices().stream()).collect(toSet());

    // TODO: make configurable
    SymmetricKeyGenerator symmetricKeyGenerator = new AesSymmetricKeyGenerator(monitor);
    AsymmetricKeyGenerator asymmetricKeyGenerator = new RsaSymmetricKeyGenerator(monitor);

    JobHooks jobHooks = loadJobHooks();

    Injector injector =
        Guice.createInjector(
            new WorkerModule(
                extensionContext,
                cloudExtension,
                transferExtensions,
                publicKeySerializers,
                decryptServices,
                symmetricKeyGenerator,
                asymmetricKeyGenerator,
                jobHooks));
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
    ServiceLoader.load(TransferExtension.class).iterator()
        .forEachRemaining(ext -> {
          monitor.info(() -> "Loading transfer extension: " + ext + " for " + ext.getServiceId());
          extensionsBuilder.add(ext);
        });
    ImmutableList<TransferExtension> extensions = extensionsBuilder.build();
    Preconditions.checkState(
        !extensions.isEmpty(), "Could not find any implementations of TransferExtension");
    return extensions;
  }
}
