package org.dataportabilityproject.transfer.microsoft.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import ezvcard.VCard;
import ezvcard.io.json.JCardWriter;
import ezvcard.property.StructuredName;
import okhttp3.OkHttpClient;
import org.dataportabilityproject.auth.microsoft.harness.AuthTestDriver;
import org.dataportabilityproject.spi.transfer.provider.ImportResult;
import org.dataportabilityproject.spi.transfer.provider.Importer;
import org.dataportabilityproject.transfer.microsoft.provider.MicrosoftTransferServiceProvider;
import org.dataportabilityproject.transfer.microsoft.transformer.TransformerService;
import org.dataportabilityproject.transfer.microsoft.transformer.TransformerServiceImpl;
import org.dataportabilityproject.types.transfer.auth.TokenAuthData;
import org.dataportabilityproject.types.transfer.models.contacts.ContactsModelWrapper;

import java.io.IOException;
import java.io.StringWriter;

/**
 *
 */
public class LocalImportTestRunner {
    @SuppressWarnings("unchecked")
    public static void main(String... args) throws Exception {
        AuthTestDriver authTestDriver = new AuthTestDriver();

        OkHttpClient client = new OkHttpClient.Builder().build();

        ObjectMapper mapper = new ObjectMapper();

        TransformerService transformerService = new TransformerServiceImpl();

        MicrosoftTransferServiceProvider serviceProvider = new MicrosoftTransferServiceProvider(client, mapper, transformerService);
        TokenAuthData token = authTestDriver.getOAuthTokenCode();

        Importer<TokenAuthData, ContactsModelWrapper> contacts = (Importer<TokenAuthData, ContactsModelWrapper>) serviceProvider.getImporter("contacts");

        ContactsModelWrapper wrapper = new ContactsModelWrapper(createCards());
        ImportResult result = contacts.importItem(token, wrapper);

    }

    private static String createCards() throws IOException {
        StringWriter stringWriter = new StringWriter();
        JCardWriter writer = new JCardWriter(stringWriter);

        VCard card1 = new VCard();
        StructuredName structuredName1 = new StructuredName();
        structuredName1.setGiven("Test Given Data1");
        structuredName1.setFamily("Test Surname Data1");
        card1.setStructuredName(structuredName1);

        VCard card2 = new VCard();
        StructuredName structuredName2 = new StructuredName();
        structuredName2.setGiven("Test Given Data2");
        structuredName2.setFamily("Test Surname Data2");
        card2.setStructuredName(structuredName2);
        
        writer.write(card1);
        writer.write(card2);
        writer.close();
        return stringWriter.toString();
    }

}
