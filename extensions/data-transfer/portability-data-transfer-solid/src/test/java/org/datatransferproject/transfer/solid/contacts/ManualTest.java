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
package org.datatransferproject.transfer.solid.contacts;

import com.google.common.collect.ImmutableList;
import org.datatransferproject.spi.transfer.idempotentexecutor.IdempotentImportExecutor;
import org.datatransferproject.test.types.FakeIdempotentImportExecutor;
import org.datatransferproject.transfer.solid.SolidUtilities;
import org.datatransferproject.transfer.solid.SslHelper;
import org.datatransferproject.types.common.models.contacts.ContactsModelWrapper;
import org.datatransferproject.types.transfer.auth.CookiesAndUrlAuthData;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.UUID;

public class ManualTest {

    // URL to your POD
    private static final String ROOT_URL = "FILL_ME_IN";

    // See the SolidAuthSetup.md file for details on how to generate these.
    // Path to your local pkcs12 key.
    private static final String PATH_TO_CLIENT_CERT = "FILL_ME_IN";

    // Password your pkcs12 key is encrypted with.
    private static final String CLIENT_CERT_PASSWORD = "FILL_ME_IN";

    private final CookiesAndUrlAuthData authData;

    private final SolidUtilities solidUtilities;

    private final IdempotentImportExecutor executor;

    public static void main(String[] args) throws Exception {
        new ManualTest().reset().manualImport();
    }

    private ManualTest() throws GeneralSecurityException, IOException {
        String authCookie = new SslHelper(PATH_TO_CLIENT_CERT, CLIENT_CERT_PASSWORD).loginViaCertificate();
        this.authData = new CookiesAndUrlAuthData(ImmutableList.of(authCookie), ROOT_URL);
        this.solidUtilities = new SolidUtilities(authCookie);
        this.executor = new FakeIdempotentImportExecutor();
    }

    ManualTest manualImport() throws Exception {
        SolidContactsImport importer = new SolidContactsImport();
        importer.importItem(UUID.randomUUID(), executor, this.authData, new ContactsModelWrapper(TestData.VCARD_TEXT));
        return this;
    }

    ManualTest reset() throws Exception {
        solidUtilities.recursiveDelete(ROOT_URL + SolidContactsImport.IMPORTED_ADDRESS_BOOK_PATH);
        return this;
    }
}
