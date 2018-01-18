/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.dataportabilityproject.serviceProviders.microsoft.mail;

import java.io.IOException;
import java.util.Optional;
import javax.annotation.Nullable;
import javax.mail.MessagingException;
import org.dataportabilityproject.dataModels.ExportInformation;
import org.dataportabilityproject.dataModels.Exporter;
import org.dataportabilityproject.dataModels.Importer;
import org.dataportabilityproject.dataModels.PaginationInformation;
import org.dataportabilityproject.dataModels.Resource;
import org.dataportabilityproject.dataModels.mail.MailModelWrapper;
import org.dataportabilityproject.serviceProviders.common.mail.ImapMailHelper;
import org.dataportabilityproject.shared.IdOnlyResource;

public class MicrosoftMailService implements Exporter<MailModelWrapper>,
    Importer<MailModelWrapper> {

  public static final String HOST = "outlook.office365.com";
  public static final String PROTOCOL = "imap";
  private final String account;
  private final String password;

  public MicrosoftMailService(String account, String password) {
    this.account = account;
    this.password = password;
  }

  @Nullable
  private static PaginationInformation getPaginationInformation(
      ExportInformation exportInformation) {
    return exportInformation.getPaginationInformation().orElse(null);
  }

  @Override
  public void importItem(MailModelWrapper object) throws IOException {
    System.out.println("Microsoft Mail Service import not implemented");
  }

  @Override
  public MailModelWrapper export(ExportInformation exportInformation)
      throws IOException {
    ImapMailHelper helper = new ImapMailHelper();
    Optional<Resource> resource = exportInformation.getResource();
    try {
      if (resource.isPresent()) {
        IdOnlyResource folder = (IdOnlyResource) resource.get();

        return helper.getFolderContents(HOST, account, password, folder.getId(),
            getPaginationInformation(exportInformation));
      } else {
        return helper.getFolderContents(HOST, account, password, null,
            getPaginationInformation(exportInformation));
      }
    } catch (MessagingException e) {
      throw new IOException(e);
    }
  }
}
