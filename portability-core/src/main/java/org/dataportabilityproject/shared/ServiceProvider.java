package org.dataportabilityproject.shared;

import com.google.common.collect.ImmutableList;
import org.dataportabilityproject.cloud.interfaces.JobDataCache;
import org.dataportabilityproject.dataModels.DataModel;
import org.dataportabilityproject.dataModels.Exporter;
import org.dataportabilityproject.dataModels.Importer;

import java.io.IOException;
import org.dataportabilityproject.shared.auth.AuthData;
import org.dataportabilityproject.shared.auth.OfflineAuthDataGenerator;
import org.dataportabilityproject.shared.auth.OnlineAuthDataGenerator;

/**
 * A service provider that supports importing and export different data types.
 */
public interface ServiceProvider {
  String getName();

  ImmutableList<PortableDataType> getExportTypes();

  ImmutableList<PortableDataType> getImportTypes();

  OfflineAuthDataGenerator getOfflineAuthDataGenerator(PortableDataType dataType);

  default OnlineAuthDataGenerator getOnlineAuthDataGenerator(PortableDataType dataType) {
    System.out.println("WARNING: getOnlineAuthDataGenerator not implemented");
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
