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
import org.dataportabilityproject.shared.auth.AuthData;

public class PortabilityMain {
    private static final IOInterface IO_INTERFACE = new ConsoleIO();

    public static void main(String[] args) throws Exception {
        Secrets secrets = new Secrets("secrets.csv");
        ServiceProviderRegistry registry = new ServiceProviderRegistry(secrets);

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

        AuthData exportAuthData = registry.getOfflineAuth(exporterName, type)
            .generateAuthData(IO_INTERFACE);
        Exporter<T> exporter = registry.getExporter(exporterName, type, exportAuthData);

        // This is a hack to allow round tripping to the same account while only doing one auth.
        AuthData importAuthData;
        if (exporterName.equals(importerName)) {
            importAuthData = exportAuthData;
        } else {
            importAuthData = registry.getOfflineAuth(importerName, type)
                .generateAuthData(IO_INTERFACE);
        }

        Importer<T> importer = registry.getImporter(importerName, type, importAuthData);
        ExportInformation emptyExportInfo =
            new ExportInformation(Optional.empty(), Optional.empty());

        PortabilityCopier.copyDataType(registry, type, exporterName, exportAuthData, importerName, importAuthData);;
    }

}
