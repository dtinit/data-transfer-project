package org.dataportabilityproject.transfer.microsoft.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import org.dataportabilityproject.auth.microsoft.harness.AuthTestDriver;
import org.dataportabilityproject.spi.transfer.provider.ExportResult;
import org.dataportabilityproject.spi.transfer.provider.Exporter;
import org.dataportabilityproject.transfer.microsoft.provider.MicrosoftTransferServiceProvider;
import org.dataportabilityproject.transfer.microsoft.transformer.TransformerService;
import org.dataportabilityproject.transfer.microsoft.transformer.TransformerServiceImpl;
import org.dataportabilityproject.types.transfer.auth.TokenAuthData;
import org.dataportabilityproject.types.transfer.models.contacts.ContactsModelWrapper;

/**
 *
 */
public class LocalExportTestRunner {

    @SuppressWarnings("unchecked")
    public static void main(String... args) throws Exception {
        AuthTestDriver authTestDriver = new AuthTestDriver();

        OkHttpClient client = new OkHttpClient.Builder().build();

        ObjectMapper mapper = new ObjectMapper();

        TransformerService transformerService = new TransformerServiceImpl();

        MicrosoftTransferServiceProvider serviceProvider = new MicrosoftTransferServiceProvider(client, mapper, transformerService);
        TokenAuthData token = authTestDriver.getOAuthTokenCode();

        Exporter<TokenAuthData, ContactsModelWrapper> contacts = (Exporter<TokenAuthData, ContactsModelWrapper>) serviceProvider.getExporter("contacts");
        ExportResult<ContactsModelWrapper> wrapper = contacts.export(token);

    }

}
