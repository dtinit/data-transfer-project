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
import com.google.api.services.gmail.model.Label;
import com.google.api.services.gmail.model.ListLabelsResponse;
import com.google.api.services.gmail.model.Message;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.datatransfer.google.common.GoogleCredentialFactory;
import org.datatransferproject.datatransfer.google.common.GoogleStaticObjects;
import org.datatransferproject.spi.transfer.idempotentexecutor.IdempotentImportExecutor;
import org.datatransferproject.spi.transfer.provider.ImportResult;
import org.datatransferproject.spi.transfer.provider.Importer;
import org.datatransferproject.types.common.models.mail.MailContainerModel;
import org.datatransferproject.types.common.models.mail.MailContainerResource;
import org.datatransferproject.types.common.models.mail.MailMessageModel;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;


public class GoogleMailImporter implements Importer<TokensAndUrlAuthData, MailContainerResource> {
  @VisibleForTesting
  // The special value me can be used to indicate the authenticated user to the gmail api
  static final String USER = "me";

  @VisibleForTesting static final String LABEL = "DTP-migrated";

  private GoogleCredentialFactory credentialFactory;
  private final Gmail gmail;
  private final Monitor monitor;

  public GoogleMailImporter(GoogleCredentialFactory credentialFactory, Monitor monitor) {
    this(credentialFactory, null, monitor);
  }

  @VisibleForTesting
  GoogleMailImporter(
      GoogleCredentialFactory credentialFactory, Gmail gmail, Monitor monitor) {
    this.credentialFactory = credentialFactory;
    this.gmail = gmail;
    this.monitor = monitor;
  }

  @Override
  public ImportResult importItem(
      UUID id,
      IdempotentImportExecutor idempotentExecutor,
      TokensAndUrlAuthData authData,
      MailContainerResource data) throws Exception {

    // Lazy init the request for all labels in the destination account, since it may not be needed
    // Mapping of labelName -> destination label id
    Supplier<Map<String, String>> allDestinationLabels = allDestinationLabelsSupplier(authData);

    // Import folders/labels
    importLabels(authData, idempotentExecutor, allDestinationLabels, data.getFolders());


    // Import the special DTP label
    importDTPLabel(authData, idempotentExecutor, allDestinationLabels);

    // Import labels from the given set of messages
    importLabelsForMessages(
            authData, idempotentExecutor, allDestinationLabels, data.getMessages());

    importMessages(authData, idempotentExecutor, data.getMessages());

    return ImportResult.OK;
  }

  /**
   * Creates a label in the import account, if it doesn't already exist, for all {@code folders} .
   */
  private void importLabels(
      TokensAndUrlAuthData authData,
      IdempotentImportExecutor idempotentExecutor,
      Supplier<Map<String, String>> allDestinationLabels,
      Collection<MailContainerModel> folders) throws Exception {
    for (MailContainerModel mailContainerModel : folders) {
      Preconditions.checkArgument(!Strings.isNullOrEmpty(mailContainerModel.getName()));
      String exportedLabelName = mailContainerModel.getName();
      idempotentExecutor.executeAndSwallowIOExceptions(
          exportedLabelName,
          "Label - " + exportedLabelName,
          () -> {
            String importerLabelId = allDestinationLabels.get().get(mailContainerModel.getName());
            if (importerLabelId == null) {
              importerLabelId = createImportedLabelId(authData, mailContainerModel.getName());
            }
            return importerLabelId;
          });
      }
  }

  /** Creates a label in the import account to associate with all imported messages. */
  private void importDTPLabel(
      TokensAndUrlAuthData authData,
      IdempotentImportExecutor idempotentExecutor,
      Supplier<Map<String, String>> allDestinationLabels) throws Exception {
    idempotentExecutor.executeAndSwallowIOExceptions(
        LABEL,
        LABEL,
        () -> {
          String migratedLabelId = allDestinationLabels.get().get(LABEL);
          if (migratedLabelId == null) {
            migratedLabelId = createImportedLabelId(authData, LABEL);
          }
          return migratedLabelId;
        });
  }

  /**
   * Creates a label in the import account, if it doesn't already exist, for all labels associated
   * with the give {@code messages} .
   */
  private void importLabelsForMessages(
      TokensAndUrlAuthData authData,
      IdempotentImportExecutor idempotentExecutor,
      Supplier<Map<String, String>> allDestinationLabels,
      Collection<MailMessageModel> messages) throws Exception {
    for (MailMessageModel mailMessageModel : messages) {
      // Get or create label ids associated with this message
      for (String exportedLabelName : mailMessageModel.getContainerIds()) {
        idempotentExecutor.executeAndSwallowIOExceptions(
            exportedLabelName,
            exportedLabelName,
            () -> {
              String importerLabelId = allDestinationLabels.get().get(exportedLabelName);
              // Found no existing map or label named the same, create a new one
              if (importerLabelId == null) {
                  importerLabelId = createImportedLabelId(authData, exportedLabelName);
              }
              return importerLabelId;
            });
      }
    }
  }

  /**
   * Import each message in {@code messages} into the import account with it's associated labels.
   */
  private void importMessages(
      TokensAndUrlAuthData authData,
      IdempotentImportExecutor idempotentExecutor,
      Collection<MailMessageModel> messages) throws Exception {
    for (MailMessageModel mailMessageModel : messages) {
      idempotentExecutor.executeAndSwallowIOExceptions(
          mailMessageModel.toString(),
          // Trim the full mail message to try to give some context to the user but not overwhelm
          // them.
          "Mail message: " + mailMessageModel.getRawString()
              .substring(0, Math.min(50, mailMessageModel.getRawString().length())),
          () -> {
            // Gather the label ids that will be associated with this message
            ImmutableList.Builder<String> importedLabelIds = ImmutableList.builder();
            for (String exportedLabelIdOrName : mailMessageModel.getContainerIds()) {
              // By this time all the label ids have been added to tempdata
              String importedLabelId = idempotentExecutor.getCachedValue(exportedLabelIdOrName);
              if (importedLabelId != null) {
                importedLabelIds.add(exportedLabelIdOrName);
              } else {
                // TODO remove after testing
                monitor.debug(
                    () -> "labels should have been added prior to importing messages");
              }
            }
            // Create the message to import
            Message newMessage =
                new Message()
                    .setRaw(mailMessageModel.getRawString())
                    .setLabelIds(importedLabelIds.build());
            return getOrCreateGmail(authData)
                .users()
                .messages()
                .insert(USER, newMessage)
                .execute()
                .getId();
          });
    }
  }

  /** Supplies a mapping of Label Name -> Label Id (in the import account). */
  private java.util.function.Supplier<Map<String, String>> allDestinationLabelsSupplier(
      TokensAndUrlAuthData authData) {
    return () -> {
      ListLabelsResponse response;
      try {
        response = getOrCreateGmail(authData).users().labels().list(USER).execute();
      } catch (IOException e) {
        throw new RuntimeException("Unable to list labels for user", e);
      }
      ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();

      for (Label label : response.getLabels()) {
        // TODO: remove system labels
        builder.put(label.getName(), label.getId());
      }
      return builder.build();
    };
  }

  /** Creates the given {@code labelName} in the import service provider and returns the id. */
  private String createImportedLabelId(TokensAndUrlAuthData authData, String labelName)
      throws IOException {
    Label newLabel =
        new Label()
            .setName(labelName)
            .setLabelListVisibility("labelShow")
            .setMessageListVisibility("show");
    return getOrCreateGmail(authData).users().labels().create(USER, newLabel).execute().getId();
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
