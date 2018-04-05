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

package org.dataportabilityproject.datatransfer.google.mail;

import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.ClientParametersAuthentication;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.Gmail.Users.Messages;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;
import com.google.common.annotations.VisibleForTesting;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.dataportabilityproject.datatransfer.google.common.GoogleCredentialFactory;
import org.dataportabilityproject.datatransfer.google.common.GoogleStaticObjects;
import org.dataportabilityproject.spi.transfer.provider.ExportResult;
import org.dataportabilityproject.spi.transfer.provider.ExportResult.ResultType;
import org.dataportabilityproject.spi.transfer.provider.Exporter;
import org.dataportabilityproject.spi.transfer.types.ContinuationData;
import org.dataportabilityproject.spi.transfer.types.ExportInformation;
import org.dataportabilityproject.spi.transfer.types.IdOnlyContainerResource;
import org.dataportabilityproject.spi.transfer.types.PaginationData;
import org.dataportabilityproject.spi.transfer.types.StringPaginationToken;
import org.dataportabilityproject.types.transfer.auth.AppCredentials;
import org.dataportabilityproject.types.transfer.auth.TokensAndUrlAuthData;
import org.dataportabilityproject.types.transfer.models.DataModel;
import org.dataportabilityproject.types.transfer.models.mail.MailContainerResource;
import org.dataportabilityproject.types.transfer.models.mail.MailMessageModel;

public class GoogleMailExporter implements Exporter<TokensAndUrlAuthData,DataModel> {
  private static final long PAGE_SIZE = 50; // TODO configure this in production
  private static final String USER = "me";

  private final GoogleCredentialFactory credentialFactory;
  private volatile Gmail gmail;

  public GoogleMailExporter(GoogleCredentialFactory credentialFactory) {
    this.credentialFactory = credentialFactory;
    this.gmail = null;
  }

  @VisibleForTesting
  GoogleMailExporter(GoogleCredentialFactory credentialFactory, Gmail gmail) {
    this.credentialFactory = credentialFactory;
    this.gmail = gmail;
  }

  @Override
  public ExportResult<DataModel> export(UUID id, TokensAndUrlAuthData authData) {
    return export(id, authData, new ExportInformation(null, null));
  }

  @Override
  public ExportResult<DataModel> export(UUID id,
      TokensAndUrlAuthData authData, ExportInformation exportInformation) {
    // Create a new gmail service for the authorized user
    Gmail gmail = getOrCreateGmail(authData);

    Messages.List request = null;
    try {
      request = gmail.users().messages().list(USER).setMaxResults(PAGE_SIZE);
    } catch (IOException e) {
      return new ExportResult<>(
          ExportResult.ResultType.ERROR, "Error creating request: " + e.getMessage());
    }

    if (exportInformation.getPaginationData() != null) {
      request.setPageToken(
          ((StringPaginationToken) exportInformation.getPaginationData()).getToken());
    }

    ListMessagesResponse response = null;
    try {
      response = request.execute();
    } catch (IOException e) {
      return new ExportResult<>(
          ExportResult.ResultType.ERROR, "Error exporting messages: " + e.getMessage());
    }

    List<MailMessageModel> results = new ArrayList<>(response.getMessages().size());
    // TODO: this is a good indication we need to swap the interface
    // as we can't store all the mail messagess in memory at once.
    for (Message listMessage : response.getMessages()) {
      Message getResponse = null;
      try {
        getResponse =
            gmail.users().messages().get(USER, listMessage.getId()).setFormat("raw").execute();
      } catch (IOException e) {
        return new ExportResult<>(
            ExportResult.ResultType.ERROR, "Error exporting single message: " + e.getMessage());
      }
      // TODO: note this doesn't transfer things like labels
      results.add(new MailMessageModel(getResponse.getRaw(), getResponse.getLabelIds()));
    }

    PaginationData newPage = null;
    ResultType resultType = ResultType.END;
    if (response.getNextPageToken() != null) {
      newPage = new StringPaginationToken(response.getNextPageToken());
      resultType = ResultType.CONTINUE;
    }

    MailContainerResource mailContainerResource = new MailContainerResource(null, results);
    return new ExportResult<>(resultType, mailContainerResource, new ContinuationData(newPage));
  }

  private Gmail getOrCreateGmail(TokensAndUrlAuthData authData) {
    return gmail == null ? makeGmailService(authData) : gmail;
  }

  private synchronized Gmail makeGmailService(TokensAndUrlAuthData authData) {
    Credential credential = credentialFactory.createCredential(authData);
    return new Gmail.Builder(
            credentialFactory.getHttpTransport(), credentialFactory.getJsonFactory(), credential)
        .setApplicationName(GoogleStaticObjects.APP_NAME)
        .build();
  }
}
