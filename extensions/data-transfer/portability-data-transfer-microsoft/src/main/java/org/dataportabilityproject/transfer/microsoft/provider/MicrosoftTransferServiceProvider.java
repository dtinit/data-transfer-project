package org.dataportabilityproject.transfer.microsoft.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import org.dataportabilityproject.spi.transfer.provider.Exporter;
import org.dataportabilityproject.spi.transfer.provider.Importer;
import org.dataportabilityproject.spi.transfer.provider.TransferServiceProvider;
import org.dataportabilityproject.transfer.microsoft.contacts.MicrosoftContactsExporter;
import org.dataportabilityproject.transfer.microsoft.contacts.MicrosoftContactsImporter;
import org.dataportabilityproject.transfer.microsoft.transformer.TransformerService;

import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class MicrosoftTransferServiceProvider implements TransferServiceProvider {
    private static final String CONTACTS = "contacts"; // TODO we should standardize the naming scheme if it isn't already

    private Map<String, Exporter<?, ?>> exporterCache = new HashMap<>();
    private Map<String, Importer<?, ?>> importerCache = new HashMap<>();

    public MicrosoftTransferServiceProvider(OkHttpClient client, ObjectMapper mapper, TransformerService transformerService) {
        exporterCache.put(CONTACTS, new MicrosoftContactsExporter(client, mapper, transformerService));
        importerCache.put(CONTACTS, new MicrosoftContactsImporter(client, mapper, transformerService));
    }

    @Override
    public String getServiceId() {
        return "microsoft";
    }

    @Override
    public Exporter<?, ?> getExporter(String transferDataType) {
        return exporterCache.computeIfAbsent(transferDataType, v -> {
            throw new IllegalArgumentException("Unsupported export type: " + transferDataType);
        });
    }

    @Override
    public Importer<?, ?> getImporter(String transferDataType) {
        return importerCache.computeIfAbsent(transferDataType, v -> {
            throw new IllegalArgumentException("Unsupported import type: " + transferDataType);
        });
    }
}
