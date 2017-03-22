package org.dataportabilityproject;

import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.Collection;
import org.dataportabilityproject.dataModels.DataModel;
import org.dataportabilityproject.dataModels.Exporter;
import org.dataportabilityproject.dataModels.Importer;
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
                "What service do you want to export from",
                registry.getServiceProvidersThatCanImport(type));

        Importer<T> importer = registry.getImporter(importerName, type);
        Exporter<T> exporter = registry.getExporter(exporterName, type);
        copy(exporter, importer);
    }

    private static <T extends DataModel> void copy(
            Exporter<T> exporter,
            Importer<T> importer) throws IOException {
        Collection<T> items = exporter.export();
        for (T item : items) {
            importer.importItem(item);
        }
    }
}