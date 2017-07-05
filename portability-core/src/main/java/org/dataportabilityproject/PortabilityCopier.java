package org.dataportabilityproject;

import java.io.IOException;
import java.util.Optional;
import org.dataportabilityproject.dataModels.ContinuationInformation;
import org.dataportabilityproject.dataModels.DataModel;
import org.dataportabilityproject.dataModels.ExportInformation;
import org.dataportabilityproject.dataModels.Exporter;
import org.dataportabilityproject.dataModels.Importer;
import org.dataportabilityproject.dataModels.Resource;
import org.dataportabilityproject.shared.PortableDataType;
import org.dataportabilityproject.shared.auth.AuthData;

public class PortabilityCopier {

    // Start the copy data process
    public static <T extends DataModel> void copyDataType(ServiceProviderRegistry registry,
        PortableDataType dataType,
        String exportService,
        AuthData exportAuthData,
        String importService,
        AuthData importAuthData,
        String jobId) throws IOException {

        Exporter<T> exporter = registry.getExporter(exportService, dataType, jobId, exportAuthData);
        Importer<T> importer = registry.getImporter(importService, dataType, jobId, importAuthData);
        ExportInformation emptyExportInfo =
            new ExportInformation(Optional.empty(), Optional.empty());
        copy(exporter, importer, emptyExportInfo);
    }

    private static <T extends DataModel> void copy(
        Exporter<T> exporter,
        Importer<T> importer,
        ExportInformation exportInformation) throws IOException {

        // NOTE: order is important bellow, do the import of all the items, then do continuation
        // then do sub resources, this ensures all parents are populated before children get
        // processed.

        T items = exporter.export(exportInformation);
        importer.importItem(items);

        ContinuationInformation continuationInfo = items.getContinuationInformation();
        if (null != continuationInfo) {
            if (null != continuationInfo.getPaginationInformation()) {
                copy(exporter, importer,
                    new ExportInformation(
                        exportInformation.getResource(),
                        Optional.of(continuationInfo.getPaginationInformation())));
            }

            if (continuationInfo.getSubResources() != null) {
                for (Resource resource : continuationInfo.getSubResources()) {
                    copy(
                        exporter,
                        importer,
                        new ExportInformation(Optional.of(resource), Optional.empty()));
                }
            }
        }
    }


}
