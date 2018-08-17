package org.datatransferproject.transfer.microsoft.integration;

import java.util.Optional;
import org.datatransferproject.auth.microsoft.harness.AuthTestDriver;
import org.datatransferproject.spi.transfer.provider.ExportResult;
import org.datatransferproject.spi.transfer.provider.Exporter;
import org.datatransferproject.transfer.microsoft.MicrosoftTransferExtension;
import org.datatransferproject.types.transfer.auth.TokenAuthData;
import org.datatransferproject.types.transfer.models.contacts.ContactsModelWrapper;

import java.util.UUID;

/** Runs a contacts export using a local setup. */
@Deprecated
public class LocalExportTestRunner {

  @SuppressWarnings("unchecked")
  public static void main(String... args) throws Exception {
    AuthTestDriver authTestDriver = new AuthTestDriver();

    MicrosoftTransferExtension serviceProvider = new MicrosoftTransferExtension();
    TokenAuthData token = authTestDriver.getOAuthTokenCode();

    Exporter<TokenAuthData, ContactsModelWrapper> contacts =
        (Exporter<TokenAuthData, ContactsModelWrapper>) serviceProvider.getExporter("CONTACTS");
    ExportResult<ContactsModelWrapper> wrapper = contacts.export(UUID.randomUUID(), token,
        Optional.empty());
  }
}
