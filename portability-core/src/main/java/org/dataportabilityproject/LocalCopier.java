/*
* Copyright 2017 The Data-Portability Project Authors.
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
package org.dataportabilityproject;

import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.UUID;
import javax.inject.Inject;
import org.dataportabilityproject.cloud.interfaces.CloudFactory;
import org.dataportabilityproject.dataModels.DataModel;
import org.dataportabilityproject.shared.IOInterface;
import org.dataportabilityproject.shared.PortableDataType;
import org.dataportabilityproject.shared.ServiceMode;
import org.dataportabilityproject.shared.auth.AuthData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class LocalCopier {
  private final Logger logger = LoggerFactory.getLogger(LocalCopier.class);
  private final IOInterface ioInterface;
  private final ServiceProviderRegistry serviceRegistry;
  private final CloudFactory cloudFactory;

  @Inject
  LocalCopier(
      IOInterface ioInterface,
      ServiceProviderRegistry serviceRegistry,
      CloudFactory cloudFactory){
    this.ioInterface = ioInterface;
    this.serviceRegistry = serviceRegistry;
    this.cloudFactory = cloudFactory;
  }

  public void copyData() throws IOException {
    PortableDataType type = ioInterface.ask(
        "What data type would you like to export",
        ImmutableList.copyOf(serviceRegistry.getSupportedTypes()));

    copyDataType(serviceRegistry, type);
  }

  private <T extends DataModel> void copyDataType(
      ServiceProviderRegistry registry,
      PortableDataType type) throws IOException {

    String exporterName = ioInterface.ask(
        "What service do you want to export from",
        registry.getServiceProvidersThatCanExport(type));
    String importerName = ioInterface.ask(
        "What service do you want to import to",
        registry.getServiceProvidersThatCanImport(type));

    AuthData exportAuthData = registry.getOfflineAuth(exporterName, type, ServiceMode.EXPORT)
        .generateAuthData(ioInterface);

    // This is a hack to allow round tripping to the same account while only doing one auth.
    AuthData importAuthData;
    if (exporterName.equals(importerName)) {
      importAuthData = exportAuthData;
    } else {
      importAuthData = registry.getOfflineAuth(importerName, type, ServiceMode.IMPORT)
          .generateAuthData(ioInterface);
    }

    String jobId = UUID.randomUUID().toString();

    try {
      logger.info("Starting job {}", jobId);
      PortabilityCopier.copyDataType(registry, type, exporterName, exportAuthData,
          importerName, importAuthData, jobId);
    } finally {
      cloudFactory.clearJobData(jobId);
    }
  }
}
