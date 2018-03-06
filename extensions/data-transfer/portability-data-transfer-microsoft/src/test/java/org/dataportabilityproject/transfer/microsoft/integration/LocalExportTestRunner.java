package org.dataportabilityproject.transfer.microsoft.integration;

import org.dataportabilityproject.auth.microsoft.harness.AuthTestDriver;
import org.dataportabilityproject.spi.transfer.provider.ExportResult;
import org.dataportabilityproject.spi.transfer.provider.Exporter;
import org.dataportabilityproject.transfer.microsoft.provider.MicrosoftTransferServiceProvider;
import org.dataportabilityproject.types.transfer.auth.TokenAuthData;
import org.dataportabilityproject.types.transfer.models.contacts.ContactsModelWrapper;

/**
 * Runs a contacts export using a local setup.
 */
@Deprecated
public class LocalExportTestRunner {

    @SuppressWarnings("unchecked")
    public static void main(String... args) throws Exception {
        AuthTestDriver authTestDriver = new AuthTestDriver();

        MicrosoftTransferServiceProvider serviceProvider = new MicrosoftTransferServiceProvider();
        TokenAuthData token = authTestDriver.getOAuthTokenCode();

        Exporter<TokenAuthData, ContactsModelWrapper> contacts =
            (Exporter<TokenAuthData, ContactsModelWrapper>) serviceProvider.getExporter("contacts");
        ExportResult<ContactsModelWrapper> wrapper = contacts.export(token);

    }

}
