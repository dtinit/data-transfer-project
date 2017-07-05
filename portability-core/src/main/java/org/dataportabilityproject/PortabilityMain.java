package org.dataportabilityproject;

import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.UUID;
import org.dataportabilityproject.cloud.interfaces.CloudFactory;
import org.dataportabilityproject.cloud.local.LocalCloudFactory;
import org.dataportabilityproject.dataModels.DataModel;
import org.dataportabilityproject.shared.IOInterface;
import org.dataportabilityproject.shared.PortableDataType;
import org.dataportabilityproject.shared.Secrets;
import org.dataportabilityproject.shared.auth.AuthData;

public class PortabilityMain {
    private static final IOInterface IO_INTERFACE = new ConsoleIO();
    private static CloudFactory cloudFactory = new LocalCloudFactory();

    public static void main(String[] args) throws Exception {
        Secrets secrets = new Secrets("secrets.csv");
        ServiceProviderRegistry registry = new ServiceProviderRegistry(secrets, cloudFactory);

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

        // This is a hack to allow round tripping to the same account while only doing one auth.
        AuthData importAuthData;
        if (exporterName.equals(importerName)) {
            importAuthData = exportAuthData;
        } else {
            importAuthData = registry.getOfflineAuth(importerName, type)
                .generateAuthData(IO_INTERFACE);
        }

        String jobId = UUID.randomUUID().toString();

        try {
            PortabilityCopier.copyDataType(registry, type, exporterName, exportAuthData,
                importerName, importAuthData, jobId);
        } finally {
            cloudFactory.clearJobData(jobId);
        }
    }

}
