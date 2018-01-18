/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.dataportabilityproject.shared;

import com.google.common.collect.ImmutableList;
import java.io.IOException;
import org.dataportabilityproject.cloud.interfaces.JobDataCache;
import org.dataportabilityproject.dataModels.DataModel;
import org.dataportabilityproject.dataModels.Exporter;
import org.dataportabilityproject.dataModels.Importer;
import org.dataportabilityproject.shared.auth.AuthData;
import org.dataportabilityproject.shared.auth.OfflineAuthDataGenerator;
import org.dataportabilityproject.shared.auth.OnlineAuthDataGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A service provider that supports importing and export different data types.
 */
public interface ServiceProvider {
    Logger logger = LoggerFactory.getLogger(ServiceProvider.class);

    String getName();

    ImmutableList<PortableDataType> getExportTypes();

    ImmutableList<PortableDataType> getImportTypes();

    /**
     * Return an OfflineAuthDataGenerator for the provided dataType. The returned generator will
     * have permissions based on the serviceMode provided.
     */
    default OfflineAuthDataGenerator getOfflineAuthDataGenerator(PortableDataType dataType, ServiceMode serviceMode) {
      logger.warn(
          "getOnlineAuthDataGenerator not implemented for type: {}, serviceMode: {}, service: {}",
          dataType, serviceMode, getName());
      return null;
    }


    /* Return an OnlineAuthDataGenerator for the provided  dataType. The returned generator will
     * have permissions based on the serviceMode provided.
     */
    default OnlineAuthDataGenerator getOnlineAuthDataGenerator(PortableDataType dataType, ServiceMode serviceMode) {
      logger.warn(
          "getOnlineAuthDataGenerator not implemented for type: {}, serviceMode: {} service: {}",
          dataType, serviceMode, getName());
      return null;
    }

    Exporter<? extends DataModel> getExporter(
            PortableDataType type,
            AuthData authData,
            JobDataCache jobDataCache) throws IOException;

    Importer<? extends DataModel> getImporter(
            PortableDataType type,
            AuthData authData,
            JobDataCache jobDataCache) throws IOException;
}