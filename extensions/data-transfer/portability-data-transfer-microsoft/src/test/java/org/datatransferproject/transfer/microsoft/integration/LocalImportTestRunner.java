/*
 * Copyright 2018 The Data Transfer Project Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.datatransferproject.transfer.microsoft.integration;

import static org.datatransferproject.types.common.models.DataVertical.CONTACTS;

import ezvcard.VCard;
import ezvcard.io.json.JCardWriter;
import ezvcard.property.StructuredName;
import org.datatransferproject.auth.microsoft.harness.AuthTestDriver;
import org.datatransferproject.spi.transfer.provider.ImportResult;
import org.datatransferproject.spi.transfer.provider.Importer;
import org.datatransferproject.test.types.FakeIdempotentImportExecutor;
import org.datatransferproject.transfer.microsoft.MicrosoftTransferExtension;
import org.datatransferproject.types.transfer.auth.TokenAuthData;
import org.datatransferproject.types.common.models.contacts.ContactsModelWrapper;

import java.io.IOException;
import java.io.StringWriter;
import java.util.UUID;

/** */
public class LocalImportTestRunner {
  @SuppressWarnings("unchecked")
  public static void main(String... args) throws Exception {
    AuthTestDriver authTestDriver = new AuthTestDriver();

    MicrosoftTransferExtension serviceProvider = new MicrosoftTransferExtension();
    TokenAuthData token = authTestDriver.getOAuthTokenCode();

    Importer<TokenAuthData, ContactsModelWrapper> contacts =
        (Importer<TokenAuthData, ContactsModelWrapper>) serviceProvider.getImporter(CONTACTS);

    ContactsModelWrapper wrapper = new ContactsModelWrapper(createCards());
    FakeIdempotentImportExecutor executor = new FakeIdempotentImportExecutor();
    ImportResult result = contacts.importItem(UUID.randomUUID(), executor, token, wrapper);
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
