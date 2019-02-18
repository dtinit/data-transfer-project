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

import static com.google.common.collect.MoreCollectors.onlyElement;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.AbstractScheduledService;
import com.google.common.util.concurrent.AbstractScheduledService.Scheduler;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.datatransferproject.api.launcher.ExtensionContext;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.config.FlagBindingModule;
import org.datatransferproject.security.AsymmetricKeyGenerator;
import org.datatransferproject.security.SymmetricKeyGenerator;
import org.datatransferproject.spi.cloud.extension.CloudExtension;
import org.datatransferproject.spi.cloud.storage.AppCredentialStore;
import org.datatransferproject.spi.cloud.storage.JobStore;
import org.datatransferproject.spi.transfer.extension.TransferExtension;
import org.datatransferproject.spi.transfer.hooks.JobHooks;
import org.datatransferproject.spi.transfer.provider.Exporter;
import org.datatransferproject.spi.transfer.provider.Importer;
import org.datatransferproject.spi.transfer.security.AuthDataDecryptService;
import org.datatransferproject.spi.transfer.security.PublicKeySerializer;
import org.datatransferproject.types.transfer.retry.RetryStrategyLibrary;

final class WorkerModule extends FlagBindingModule {

  private final CloudExtension cloudExtension;
  private final ExtensionContext context;
  private final List<TransferExtension> transferExtensions;
  private final Set<PublicKeySerializer> publicKeySerializers;
  private final Set<AuthDataDecryptService> decryptServices;
  private final SymmetricKeyGenerator symmetricKeyGenerator;
  private final AsymmetricKeyGenerator asymmetricKeyGenerator;
  private final JobHooks jobHooks;

  WorkerModule(
      ExtensionContext context,
      CloudExtension cloudExtension,
      List<TransferExtension> transferExtensions,
      Set<PublicKeySerializer> publicKeySerializers,
      Set<AuthDataDecryptService> decryptServices,
      SymmetricKeyGenerator symmetricKeyGenerator,
      AsymmetricKeyGenerator asymmetricKeyGenerator,
      JobHooks jobHooks) {
    this.cloudExtension = cloudExtension;
    this.context = context;
    this.transferExtensions = transferExtensions;
    this.publicKeySerializers = publicKeySerializers;
    this.decryptServices = decryptServices;
    this.symmetricKeyGenerator = symmetricKeyGenerator;
    this.asymmetricKeyGenerator = asymmetricKeyGenerator;
    this.jobHooks = jobHooks;
  }

  @VisibleForTesting
  static TransferExtension findTransferExtension(
      ImmutableList<TransferExtension> transferExtensions, String service) {
    try {
      return transferExtensions
          .stream()
          .filter(ext -> ext.getServiceId().toLowerCase().equals(service.toLowerCase()))
          .collect(onlyElement());
    } catch (IllegalArgumentException e) {
      throw new IllegalStateException(
          "Found multiple transfer extensions for service " + service, e);
    } catch (NoSuchElementException e) {
      throw new IllegalStateException(
          "Did not find a valid transfer extension for service " + service, e);
    }
  }

  @Override
  protected void configure() {
    // binds flags from ExtensionContext to @Named annotations
    bindFlags(context);

    bind(JobHooks.class).toInstance(jobHooks);
    bind(SymmetricKeyGenerator.class).toInstance(symmetricKeyGenerator);
    bind(AsymmetricKeyGenerator.class).toInstance(asymmetricKeyGenerator);
    bind(InMemoryDataCopier.class).to(PortabilityInMemoryDataCopier.class);
    bind(ObjectMapper.class).toInstance(context.getTypeManager().getMapper());
  }

  @Provides
  @Singleton
  JobStore getJobStore() {
    return cloudExtension.getJobStore();
  }

  @Provides
  @Singleton
  AppCredentialStore getBucketStore() {
    return cloudExtension.getAppCredentialStore();
  }

  @Provides
  @Singleton
  Exporter getExporter(ImmutableList<TransferExtension> transferExtensions) {
    TransferExtension extension =
        findTransferExtension(transferExtensions, JobMetadata.getExportService());
    extension.initialize(context);
    return extension.getExporter(JobMetadata.getDataType());
  }

  @Provides
  @Singleton
  Importer getImporter(ImmutableList<TransferExtension> transferExtensions) {
    TransferExtension extension =
        findTransferExtension(transferExtensions, JobMetadata.getImportService());
    extension.initialize(context);
    return extension.getImporter(JobMetadata.getDataType());
  }

  @Provides
  @Singleton
  ImmutableList<TransferExtension> getTransferExtensions() {
    return ImmutableList.copyOf(transferExtensions);
  }

  @Provides
  @Singleton
  Set<PublicKeySerializer> getPublicKeySerializers() {
    return ImmutableSet.copyOf(publicKeySerializers);
  }

  @Provides
  @Singleton
  Set<AuthDataDecryptService> getDecryptServices() {
    return ImmutableSet.copyOf(decryptServices);
  }

  @Provides
  @Singleton
  RetryStrategyLibrary getRetryStrategyLibrary() {
    return context.getSetting("retryLibrary", null);
  }

  @Provides
  @Singleton
  Scheduler getScheduler() {
    // TODO: parse a Duration from the settings
    long interval = context.getSetting("pollInterval", 2000); // Default: poll every 2s
    return AbstractScheduledService.Scheduler.newFixedDelaySchedule(
        0, interval, TimeUnit.MILLISECONDS);
  }

  @Provides
  @Singleton
  Monitor getMonitor() {
    return context.getMonitor();
  }

  @Provides
  @Singleton
  ExtensionContext getContext() {
    return context;
  }
}
