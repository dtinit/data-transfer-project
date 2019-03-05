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

package org.datatransferproject.datatransfer.google.mail;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.Gmail.Users.Messages;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;
import com.google.common.annotations.VisibleForTesting;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.datatransferproject.datatransfer.google.common.GoogleCredentialFactory;
import org.datatransferproject.datatransfer.google.common.GoogleStaticObjects;
import org.datatransferproject.spi.transfer.provider.ExportResult;
import org.datatransferproject.spi.transfer.provider.ExportResult.ResultType;
import org.datatransferproject.spi.transfer.provider.Exporter;
import org.datatransferproject.spi.transfer.types.ContinuationData;
import org.datatransferproject.types.common.ExportInformation;
import org.datatransferproject.types.common.PaginationData;
import org.datatransferproject.types.common.StringPaginationToken;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;
import org.datatransferproject.types.common.models.mail.MailContainerResource;
import org.datatransferproject.types.common.models.mail.MailMessageModel;

public class GoogleMailExporter implements Exporter<TokensAndUrlAuthData, MailContainerResource> {
  @VisibleForTesting
  static final long PAGE_SIZE = 50; // TODO configure this in production
  @VisibleForTesting
  // The special value me can be used to indicate the authenticated user to the gmail api
  static final String USER = "me";

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
  public ExportResult<MailContainerResource> export(UUID id,
      TokensAndUrlAuthData authData, Optional<ExportInformation> exportInformation) {
    // Create a new gmail service for the authorized user
    Gmail gmail = getOrCreateGmail(authData);

    Messages.List request = null;
    try {
      request = gmail.users().messages().list(USER).setMaxResults(PAGE_SIZE);
    } catch (IOException e) {
      return new ExportResult<>(e);
    }

    if (exportInformation.isPresent() && exportInformation.get().getPaginationData() != null) {
      request.setPageToken(
          ((StringPaginationToken) exportInformation.get().getPaginationData()).getToken());
    }

    ListMessagesResponse response = null;
    try {
      response = request.execute();
    } catch (IOException e) {
      return new ExportResult<>(e);
    }

    List<MailMessageModel> results = new ArrayList<>(response.getMessages().size());
    // TODO: this is a good indication we need to swap the interface
    // as we can't store all the mail messages in memory at once.
    for (Message listMessage : response.getMessages()) {
      Message getResponse = null;
      try {
        getResponse =
            gmail.users().messages().get(USER, listMessage.getId()).setFormat("raw").execute();
      } catch (IOException e) {
        return new ExportResult<>(e);
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
