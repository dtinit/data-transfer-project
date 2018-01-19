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
package org.dataportabilityproject.serviceProviders.fiveHundredPx;

import com.google.common.collect.ImmutableList;
import java.io.IOException;
import org.dataportabilityproject.cloud.interfaces.JobDataCache;
import org.dataportabilityproject.dataModels.DataModel;
import org.dataportabilityproject.dataModels.Exporter;
import org.dataportabilityproject.dataModels.Importer;
import org.dataportabilityproject.shared.PortableDataType;
import org.dataportabilityproject.shared.ServiceProvider;
import org.dataportabilityproject.shared.auth.AuthData;

final public class FiveHundredPxServiceProvider implements ServiceProvider {

  @Override
  public String getName() {
    return null;
  }

  @Override
  public ImmutableList<PortableDataType> getExportTypes() {
    return null;
  }

  @Override
  public ImmutableList<PortableDataType> getImportTypes() {
    return null;
  }

  @Override
  public Exporter<? extends DataModel> getExporter(PortableDataType type, AuthData authData,
      JobDataCache jobDataCache) throws IOException {
    return null;
  }

  @Override
  public Importer<? extends DataModel> getImporter(PortableDataType type, AuthData authData,
      JobDataCache jobDataCache) throws IOException {
    return null;
  }
}
