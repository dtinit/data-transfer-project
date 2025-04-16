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
import static java.lang.String.format;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.AbstractScheduledService;
import com.google.common.util.concurrent.AbstractScheduledService.Scheduler;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;
import org.datatransferproject.api.launcher.DelegatingExtensionContext;
import org.datatransferproject.api.launcher.DtpInternalMetricRecorder;
import org.datatransferproject.api.launcher.ExtensionContext;
import org.datatransferproject.api.launcher.Flag;
import org.datatransferproject.api.launcher.MetricRecorder;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.config.FlagBindingModule;
import org.datatransferproject.launcher.metrics.LoggingDtpInternalMetricRecorder;
import org.datatransferproject.launcher.metrics.ServiceAwareMetricRecorder;
import org.datatransferproject.security.SymmetricKeyGenerator;
import org.datatransferproject.spi.cloud.extension.CloudExtension;
import org.datatransferproject.spi.cloud.storage.AppCredentialStore;
import org.datatransferproject.spi.cloud.storage.JobStore;
import org.datatransferproject.spi.transfer.extension.TransferExtension;
import org.datatransferproject.spi.transfer.hooks.JobHooks;
import org.datatransferproject.spi.transfer.idempotentexecutor.IdempotentImportExecutor;
import org.datatransferproject.spi.transfer.idempotentexecutor.IdempotentImportExecutorExtension;
import org.datatransferproject.spi.transfer.provider.Exporter;
import org.datatransferproject.spi.transfer.provider.Importer;
import org.datatransferproject.spi.transfer.provider.SignalHandler;
import org.datatransferproject.spi.transfer.provider.TransferCompatibilityProvider;
import org.datatransferproject.spi.transfer.security.AuthDataDecryptService;
import org.datatransferproject.spi.transfer.security.PublicKeySerializer;
import org.datatransferproject.spi.transfer.security.SecurityExtension;
import org.datatransferproject.spi.transfer.security.TransferKeyGenerator;
import org.datatransferproject.transfer.copier.InMemoryDataCopier;
import org.datatransferproject.transfer.copier.InMemoryDataCopierClassLoader;
import org.datatransferproject.types.transfer.retry.RetryStrategyLibrary;
import org.datatransferproject.types.transfer.serviceconfig.TransferServiceConfig;

final class WorkerModule extends FlagBindingModule {

  private final CloudExtension cloudExtension;
  private final ExtensionContext context;
  private final List<TransferExtension> transferExtensions;
  private final SecurityExtension securityExtension;
  private final IdempotentImportExecutorExtension idempotentImportExecutorExtension;
  private final SymmetricKeyGenerator symmetricKeyGenerator;
  private final JobHooks jobHooks;
  private final TransferCompatibilityProvider compatibilityProvider;

  WorkerModule(
      ExtensionContext context,
      CloudExtension cloudExtension,
      List<TransferExtension> transferExtensions,
      SecurityExtension securityExtension,
      IdempotentImportExecutorExtension idempotentImportExecutorExtension,
      SymmetricKeyGenerator symmetricKeyGenerator,
      JobHooks jobHooks,
      TransferCompatibilityProvider transferCompatibilityProvider) {
    this.cloudExtension = cloudExtension;
    this.context = context;
    this.transferExtensions = transferExtensions;
    this.securityExtension = securityExtension;
    this.idempotentImportExecutorExtension = idempotentImportExecutorExtension;
    this.symmetricKeyGenerator = symmetricKeyGenerator;
    this.jobHooks = jobHooks;
    this.compatibilityProvider = transferCompatibilityProvider;
  }

  @VisibleForTesting
  static TransferExtension findTransferExtension(
      ImmutableList<TransferExtension> transferExtensions, String service) {
    try {
      return transferExtensions
          .stream()
          .filter(ext -> ext.supportsService(service))
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
    bind(InMemoryDataCopier.class).to(InMemoryDataCopierClassLoader.load());
    getMonitor()
        .info(() -> "Using InMemoryDataCopier: " + InMemoryDataCopierClassLoader.load().getName());

    bind(ObjectMapper.class).toInstance(context.getTypeManager().getMapper());

    // Ensure a DtpInternalMetricRecorder exists
    LoggingDtpInternalMetricRecorder.registerRecorderIfNeeded(context);
    bind(DtpInternalMetricRecorder.class)
        .toInstance(context.getService(DtpInternalMetricRecorder.class));
  }

  @Provides
  @Singleton
  SymmetricKeyGenerator getSymmetricKeyGenerator() {
    return symmetricKeyGenerator;
  }

  @Provides
  @Singleton
  PublicKeySerializer getPublicKeySerializer() {
    return securityExtension.getPublicKeySerializer();
  }

  @Provides
  @Singleton
  AuthDataDecryptService getAuthDataDecryptService() {
    return securityExtension.getDecryptService();
  }

  @Provides
  @Singleton
  TransferKeyGenerator getTransferKeyGenerator() {
    return securityExtension.getTransferKeyGenerator();
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
    DelegatingExtensionContext serviceSpecificContext = new DelegatingExtensionContext(context);
    serviceSpecificContext.registerOverrideService(
        MetricRecorder.class,
        new ServiceAwareMetricRecorder(
            extension.getServiceId(),
            context.getService(DtpInternalMetricRecorder.class)));
    serviceSpecificContext.registerOverrideService(
        TransferServiceConfig.class,
        getTransferServiceConfig(extension));
    extension.initialize(serviceSpecificContext);

    return compatibilityProvider.getCompatibleExporter(extension, JobMetadata.getDataType());
  }

  @Provides
  @Singleton
  Importer getImporter(ImmutableList<TransferExtension> transferExtensions) {
    TransferExtension extension =
        findTransferExtension(transferExtensions, JobMetadata.getImportService());
    DelegatingExtensionContext serviceSpecificContext = new DelegatingExtensionContext(context);
    serviceSpecificContext.registerOverrideService(
        MetricRecorder.class,
        new ServiceAwareMetricRecorder(
            extension.getServiceId(),
            context.getService(DtpInternalMetricRecorder.class)));
    serviceSpecificContext.registerOverrideService(
        TransferServiceConfig.class,
        getTransferServiceConfig(extension));
    extension.initialize(serviceSpecificContext);
    return compatibilityProvider.getCompatibleImporter(extension, JobMetadata.getDataType());
  }

  @Provides
  @Singleton
  @Annotations.ImportSignalHandler
  SignalHandler getImportSignalHandler(ImmutableList<TransferExtension> transferExtensions) {
    TransferExtension extension =
      findTransferExtension(transferExtensions, JobMetadata.getImportService());
    DelegatingExtensionContext serviceSpecificContext = new DelegatingExtensionContext(context);
    serviceSpecificContext.registerOverrideService(
      MetricRecorder.class,
      new ServiceAwareMetricRecorder(
        extension.getServiceId(),
        context.getService(DtpInternalMetricRecorder.class)));
    serviceSpecificContext.registerOverrideService(
      TransferServiceConfig.class,
      getTransferServiceConfig(extension));
    extension.initialize(serviceSpecificContext);
    extension.initialize(serviceSpecificContext);
    return extension.getSignalHandler();
  }

  @Provides
  @Singleton
  @Annotations.ExportSignalHandler
  SignalHandler getExportSignalHandler(ImmutableList<TransferExtension> transferExtensions) {
    TransferExtension extension =
      findTransferExtension(transferExtensions, JobMetadata.getExportService());
    DelegatingExtensionContext serviceSpecificContext = new DelegatingExtensionContext(context);
    serviceSpecificContext.registerOverrideService(
      MetricRecorder.class,
      new ServiceAwareMetricRecorder(
        extension.getServiceId(),
        context.getService(DtpInternalMetricRecorder.class)));
    serviceSpecificContext.registerOverrideService(
      TransferServiceConfig.class,
      getTransferServiceConfig(extension));
    extension.initialize(serviceSpecificContext);
    return extension.getSignalHandler();
  }

  @Provides
  @Singleton
  ImmutableList<TransferExtension> getTransferExtensions() {
    return ImmutableList.copyOf(transferExtensions);
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
  @Annotations.CancelScheduler
  Scheduler getCancelCheckingScheduler() {
    // TODO: parse a Duration from the settings
    long interval = context.getSetting("cancelCheckPollInterval", 60000); // Default: poll every 1m
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

  private TransferServiceConfig getTransferServiceConfig(TransferExtension ext) {
    try {
      return TransferServiceConfig.getForService(ext.getServiceId());
    } catch (IOException e) {
      throw new RuntimeException("Couldn't create config for " + ext.getServiceId(), e);
    }
  }

  @Provides
  @Singleton
  public IdempotentImportExecutor getIdempotentImportExecutor() {
    return idempotentImportExecutorExtension.getIdempotentImportExecutor(context);
  }

  @Provides
  @Singleton
  @Annotations.RetryingExecutor
  public IdempotentImportExecutor getRetryingIdempotentImportExecutor() {
    return idempotentImportExecutorExtension.getRetryingIdempotentImportExecutor(context);
  }

  @Provides
  @Named("transferSignalEnabled")
  public Boolean transferSignalEnabled() {
    return context.getSetting("transferSignalEnabled", Boolean.TRUE);
  }
}
