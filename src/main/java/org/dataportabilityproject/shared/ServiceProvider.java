package org.dataportabilityproject.shared;

import com.google.common.collect.ImmutableList;
import org.dataportabilityproject.dataModels.DataModel;
import org.dataportabilityproject.dataModels.Exporter;
import org.dataportabilityproject.dataModels.Importer;

import java.io.IOException;

/**
 * A service provider that supports importing and export different data types.
 */
public interface ServiceProvider {
    String getName();

    ImmutableList<PortableDataType> getExportTypes();

    ImmutableList<PortableDataType> getImportTypes();

    Exporter<? extends DataModel> getExporter(PortableDataType type) throws IOException;
    Importer<? extends DataModel> getImporter(PortableDataType type) throws IOException;
}
