package org.dataportabilityproject;

import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.Optional;
import org.dataportabilityproject.dataModels.ContinuationInformation;
import org.dataportabilityproject.dataModels.DataModel;
import org.dataportabilityproject.dataModels.ExportInformation;
import org.dataportabilityproject.dataModels.Exporter;
import org.dataportabilityproject.dataModels.Importer;
import org.dataportabilityproject.dataModels.Resource;
import org.dataportabilityproject.shared.IOInterface;
import org.dataportabilityproject.shared.PortableDataType;
import org.dataportabilityproject.shared.Secrets;

public class PortabilityMain {
    private static final IOInterface IO_INTERFACE = new ConsoleIO();

    public static void main(String[] args) throws Exception {
        Secrets secrets = new Secrets("secrets.csv");
        ServiceProviderRegistry registry = new ServiceProviderRegistry(secrets, IO_INTERFACE);

        PortableDataType type = IO_INTERFACE.ask(
                "What data type would you like to export",
                ImmutableList.copyOf(PortableDataType.values()));

        copyDataType(registry, type);
    }

    private static <T extends DataModel> void copyDataType(
            ServiceProviderRegistry registry,
            PortableDataType type) throws IOException {

        String exporterName = IO_INTERFACE.ask(
                "What service do you want to export from",
                registry.getServiceProvidersThatCanExport(type));
        String importerName = IO_INTERFACE.ask(
                "What service do you want to import to",
                registry.getServiceProvidersThatCanImport(type));

        Exporter<T> exporter = registry.getExporter(exporterName, type);

        Importer<T> importer = registry.getImporter(importerName, type);
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
