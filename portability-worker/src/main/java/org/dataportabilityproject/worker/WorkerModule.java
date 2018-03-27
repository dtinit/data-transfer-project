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
package org.dataportabilityproject.worker;

import static com.google.common.collect.MoreCollectors.onlyElement;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import java.util.List;
import java.util.NoSuchElementException;
import org.dataportabilityproject.api.launcher.ExtensionContext;
import org.dataportabilityproject.security.AsymmetricKeyGenerator;
import org.dataportabilityproject.security.SymmetricKeyGenerator;
import org.dataportabilityproject.spi.cloud.extension.CloudExtension;
import org.dataportabilityproject.spi.cloud.storage.AppCredentialStore;
import org.dataportabilityproject.spi.cloud.storage.JobStore;
import org.dataportabilityproject.spi.transfer.InMemoryDataCopier;
import org.dataportabilityproject.spi.transfer.extension.TransferExtension;
import org.dataportabilityproject.spi.transfer.provider.Exporter;
import org.dataportabilityproject.spi.transfer.provider.Importer;

final class WorkerModule extends AbstractModule {
  private final CloudExtension cloudExtension;
  private final ExtensionContext context;
  private final List<TransferExtension> transferExtensions;
  private final SymmetricKeyGenerator symmetricKeyGenerator;
  private final AsymmetricKeyGenerator asymmetricKeyGenerator;

  WorkerModule(
      ExtensionContext context,
      CloudExtension cloudExtension,
      List<TransferExtension> transferExtensions,
      SymmetricKeyGenerator symmetricKeyGenerator,
      AsymmetricKeyGenerator asymmetricKeyGenerator) {
    this.cloudExtension = cloudExtension;
    this.context = context;
    this.transferExtensions = transferExtensions;
    this.symmetricKeyGenerator = symmetricKeyGenerator;
    this.asymmetricKeyGenerator = asymmetricKeyGenerator;
  }

  @Override
  protected void configure() {
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

  private static TransferExtension findTransferExtension(
      ImmutableList<TransferExtension> transferExtensions, String service) {
    try {
      return transferExtensions
          .stream()
          .filter(ext -> ext.getServiceId().equals(service))
          .collect(onlyElement());
    } catch (IllegalArgumentException e) {
      throw new IllegalStateException(
          "Found multiple transfer extensions for service " + service, e);
    } catch (NoSuchElementException e) {
      throw new IllegalStateException(
          "Did not find a valid transfer extension for service " + service, e);
    }
  }
}
