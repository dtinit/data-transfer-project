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
package org.dataportabilityproject.transfer;

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

import org.dataportabilityproject.config.extension.SettingsExtension;
import org.dataportabilityproject.security.AesSymmetricKeyGenerator;
import org.dataportabilityproject.security.AsymmetricKeyGenerator;
import org.dataportabilityproject.security.RsaSymmetricKeyGenerator;
import org.dataportabilityproject.security.SymmetricKeyGenerator;
import org.dataportabilityproject.spi.cloud.extension.CloudExtension;
import org.dataportabilityproject.spi.cloud.storage.AppCredentialStore;
import org.dataportabilityproject.spi.cloud.storage.JobStore;
import org.dataportabilityproject.spi.service.extension.ServiceExtension;
import org.dataportabilityproject.spi.transfer.extension.TransferExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main class to bootstrap a portability transfer that will operate on a single job whose state is
 * held in {@link JobMetadata}.
 */
public class WorkerMain {
  private static final Logger logger = LoggerFactory.getLogger(WorkerMain.class);

  private Worker worker;

  public static void main(String[] args) {
    Thread.setDefaultUncaughtExceptionHandler(UncaughtExceptionHandlers.systemExit());

    WorkerMain workerMain = new WorkerMain();
    workerMain.initialize();
    workerMain.poll();

    System.exit(0);
  }

  public void initialize() {
    SettingsExtension settingsExtension = getSettingsExtension();
    settingsExtension.initialize(null);
    WorkerExtensionContext extensionContext = new WorkerExtensionContext(settingsExtension);

    // TODO this should be moved into a service extension
    extensionContext.registerService(HttpTransport.class, new NetHttpTransport());
    extensionContext.registerService(JsonFactory.class, new JacksonFactory());

    ServiceLoader.load(ServiceExtension.class)
        .iterator()
        .forEachRemaining(serviceExtension ->
            serviceExtension.initialize(extensionContext));

    // TODO: verify that this is the cloud extension that is specified in the configuration
    CloudExtension cloudExtension = getCloudExtension();
    cloudExtension.initialize(extensionContext);
    logger.info("Using CloudExtension: {} ", cloudExtension.getClass().getName());

    JobStore jobStore = cloudExtension.getJobStore();
    extensionContext.registerService(JobStore.class, jobStore);

    AppCredentialStore appCredentialStore = cloudExtension.getAppCredentialStore();
    extensionContext.registerService(AppCredentialStore.class, appCredentialStore);

    List<TransferExtension> transferExtensions = getTransferExtensions();

    // TODO: make configurable
    SymmetricKeyGenerator symmetricKeyGenerator = new AesSymmetricKeyGenerator();
    AsymmetricKeyGenerator asymmetricKeyGenerator = new RsaSymmetricKeyGenerator();

    Injector injector =
        Guice.createInjector(new WorkerModule(extensionContext, cloudExtension, transferExtensions,
                symmetricKeyGenerator, asymmetricKeyGenerator));
    worker = injector.getInstance(Worker.class);
  }

  public void poll() {
    worker.doWork();
  }

  private static SettingsExtension getSettingsExtension() {
    ImmutableList.Builder<SettingsExtension> extensionsBuilder = ImmutableList.builder();
    ServiceLoader.load(SettingsExtension.class).iterator()
        .forEachRemaining(extensionsBuilder::add);
    ImmutableList<SettingsExtension> extensions = extensionsBuilder.build();
    Preconditions.checkState(
        extensions.size() == 1,
        "Exactly one SettingsExtension is required, but found " + extensions.size());
    return extensions.get(0);
  }

  private static CloudExtension getCloudExtension() {
    ImmutableList.Builder<CloudExtension> extensionsBuilder = ImmutableList.builder();
    ServiceLoader.load(CloudExtension.class).iterator().forEachRemaining(extensionsBuilder::add);
    ImmutableList<CloudExtension> extensions = extensionsBuilder.build();
    Preconditions.checkState(
        extensions.size() == 1,
        "Exactly one CloudExtension is required, but found " + extensions.size());
    return extensions.get(0);
  }

  private static List<TransferExtension> getTransferExtensions() {
    // TODO: Next version should ideally not load every TransferExtension impl, look into
    // solutions where we selectively invoke class loader.
    ImmutableList.Builder<TransferExtension> extensionsBuilder = ImmutableList.builder();
    // Note that initialization of the TransferExtension is done in the WorkerModule since they're
    // initialized as they're requested.
    ServiceLoader.load(TransferExtension.class).iterator().forEachRemaining(extensionsBuilder::add);
    ImmutableList<TransferExtension> extensions = extensionsBuilder.build();
    Preconditions.checkState(
        !extensions.isEmpty(), "Could not find any implementations of TransferExtension");
    return extensions;
  }
}
