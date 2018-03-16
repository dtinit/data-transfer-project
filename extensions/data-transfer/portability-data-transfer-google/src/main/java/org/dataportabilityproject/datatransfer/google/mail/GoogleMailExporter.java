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

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Label;
import com.google.api.services.gmail.model.ListLabelsResponse;
import java.io.IOException;
import org.dataportabilityproject.datatransfer.google.common.GoogleStaticObjects;
import org.dataportabilityproject.spi.transfer.provider.ExportResult;
import org.dataportabilityproject.spi.transfer.provider.Exporter;
import org.dataportabilityproject.spi.transfer.types.ExportInformation;
import org.dataportabilityproject.types.transfer.auth.AuthData;
import org.dataportabilityproject.types.transfer.models.mail.MailContainerResource;

public class GoogleMailExporter implements Exporter<AuthData, MailContainerResource> {

  // TODO: Configure MAX_RESULTS_PER_REQUEST to a reasonable number of messages to process in memory
  // Max results to fetch on each request for more mail messages
  private static final long MAX_RESULTS_PER_REQUEST = 10L;
  private static final String USER = "me";
  private static final String LABEL = "WT-migrated";

  private volatile Gmail gmailService;

  @Override
  public ExportResult<MailContainerResource> export(AuthData authData) {
    return null;
  }

  @Override
  public ExportResult<MailContainerResource> export(AuthData authData,
      ExportInformation exportInformation) {
    return null;
  }

  private String getMigratedLabelId(AuthData authData) throws IOException {
    ListLabelsResponse response = getOrCreateGmailService(authData).users().labels().list(USER)
        .execute();
    for (Label label : response.getLabels()) {
      if (label.getName().equals(LABEL)) {
        return label.getId();
      }
    }

    Label newLabel =
        new Label()
            .setName(LABEL)
            .setLabelListVisibility("labelShow")
            .setMessageListVisibility("show");
    return getOrCreateGmailService(authData).users().labels().create(USER, newLabel).execute()
        .getId();
  }

  private Gmail getOrCreateGmailService(AuthData authData) {
    return gmailService == null ? makeGmailService(authData) : gmailService;
  }

  private synchronized Gmail makeGmailService(AuthData authData) {
    // TODO(olsona): get credential using authData
    Credential credential = null;
    return new Gmail.Builder(GoogleStaticObjects.getHttpTransport(),
        GoogleStaticObjects.JSON_FACTORY, credential)
        .setApplicationName(GoogleStaticObjects.APP_NAME)
        .build();
  }
}
