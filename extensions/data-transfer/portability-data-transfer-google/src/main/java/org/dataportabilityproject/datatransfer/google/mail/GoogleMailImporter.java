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
import com.google.api.services.gmail.model.Message;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import org.dataportabilityproject.datatransfer.google.common.GoogleCredentialFactory;
import org.dataportabilityproject.datatransfer.google.common.GoogleStaticObjects;
import org.dataportabilityproject.spi.cloud.storage.JobStore;
import org.dataportabilityproject.spi.transfer.provider.ImportResult;
import org.dataportabilityproject.spi.transfer.provider.ImportResult.ResultType;
import org.dataportabilityproject.spi.transfer.provider.Importer;
import org.dataportabilityproject.spi.transfer.types.TempMailData;
import org.dataportabilityproject.types.transfer.auth.TokensAndUrlAuthData;
import org.dataportabilityproject.types.transfer.models.mail.MailContainerModel;
import org.dataportabilityproject.types.transfer.models.mail.MailContainerResource;
import org.dataportabilityproject.types.transfer.models.mail.MailMessageModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GoogleMailImporter implements Importer<TokensAndUrlAuthData, MailContainerResource> {
  private final Logger logger = LoggerFactory.getLogger(GoogleMailImporter.class);

  @VisibleForTesting
  static final long MAX_RESULTS_PER_REQUEST = 10L;
  @VisibleForTesting
  // The special value me can be used to indicate the authenticated user to the gmail api
  static final String USER = "me";
  @VisibleForTesting
  static final String LABEL = "DTP-migrated";

  private GoogleCredentialFactory credentialFactory;
  private final JobStore jobStore;
  private final Gmail gmail;

  public GoogleMailImporter(GoogleCredentialFactory credentialFactory, JobStore jobStore) {
    this(credentialFactory, jobStore, null);
  }

  @VisibleForTesting
  GoogleMailImporter(GoogleCredentialFactory credentialFactory, JobStore jobStore, Gmail gmail) {
    this.credentialFactory = credentialFactory;
    this.jobStore = jobStore;
    this.gmail = gmail;
  }

  @Override
  public ImportResult importItem(
      UUID id, TokensAndUrlAuthData authData, MailContainerResource data) {

    // Initialize the temp storage of import folder/label mappings associated with this job
    TempMailData tempMailData;
    try {
      tempMailData = getOrCreateMailData(id);
    } catch (IOException e) {
      return new ImportResult(e);
    }

    // Lazy init the request for all labels in the destination account, since it may not be needed
    // Mapping of labelName -> destination label id
    Supplier<Map<String, String>> allDestinationLabels =
        Suppliers.memoize(allDestinationLabelsSupplier(authData));

    // Import folders/labels
    ImportResult result =
        importLabels(id, authData, tempMailData, allDestinationLabels, data.getFolders());
    if (result != ImportResult.OK) {
      logger.warn("Error after importing labels");
      return result;
    }

    // Import the special DTP label
    result = importDTPLabel(id, authData, tempMailData, allDestinationLabels);
    if (result != ImportResult.OK) {
      logger.warn("Error after importing DTP table");
      return result;
    }

    // Import labels from the given set of messages
    result =
        importLabelsForMessages(
            id, authData, tempMailData, allDestinationLabels, data.getMessages());
    if (result != ImportResult.OK) {
      logger.warn("Error after importing labels for messages");
      return result;
    }

    // Import messages
    result = importMessages(authData, tempMailData, data.getMessages());
    if (result != ImportResult.OK) {
      logger.warn("Error after importing messages");
      return result;
    }
    return result;
  }

  /**
   * Creates a label in the import account, if it doesn't already exist, for all {@code folders} .
   */
  private ImportResult importLabels(
      UUID id,
      TokensAndUrlAuthData authData,
      TempMailData tempMailData,
      Supplier<Map<String, String>> allDestinationLabels,
      Collection<MailContainerModel> folders) {
    boolean newMappingsCreated = false;
    for (MailContainerModel mailContainerModel : folders) {
      Preconditions.checkArgument(!Strings.isNullOrEmpty(mailContainerModel.getName()));
      String exportedLabelName = mailContainerModel.getName();
      // Check if we have it in temp data
      String importerLabelId = tempMailData.getImportedId(exportedLabelName);
      if (importerLabelId == null) {
        // If a label with the same name already exists in the destination account, use that
        importerLabelId = allDestinationLabels.get().get(mailContainerModel.getName());
        // Found no existing map or label named the same, create a new one
        if (importerLabelId == null) {
          try {
            importerLabelId = createImportedLabelId(authData, mailContainerModel.getName());
          } catch (IOException e) {
            return new ImportResult(e);
          }
        }
        tempMailData.addFolderIdMapping(exportedLabelName, importerLabelId);
        newMappingsCreated = true;
      }
    }

    // Persist temp data in case we added mappings along the way
    if (newMappingsCreated) {
      jobStore.update(id, createCacheKey(), tempMailData);
    }
    return ImportResult.OK;
  }

  /** Creates a label in the import account to associate with all imported messages. */
  private ImportResult importDTPLabel(
      UUID id,
      TokensAndUrlAuthData authData,
      TempMailData tempMailData,
      Supplier<Map<String, String>> allDestinationLabels) {
    boolean newMappingsCreated = false;
    // Retrieve, and optionally create on demand, the migration label id
    String migratedLabelId = tempMailData.getImportedId(LABEL);
    if (migratedLabelId == null) {
      // If a label with the same name already exists in the destination account, use that
      migratedLabelId = allDestinationLabels.get().get(LABEL);
      tempMailData.addFolderIdMapping(LABEL, migratedLabelId);
      newMappingsCreated = true;

      // Found no existing map or label named the same, create a new one
      if (migratedLabelId == null) {
        try {
          migratedLabelId = createImportedLabelId(authData, LABEL);
        } catch (IOException e) {
          return new ImportResult(e);
        }
        tempMailData.addFolderIdMapping(LABEL, migratedLabelId);
        newMappingsCreated = true;
      }
    }

    // Persist temp data in case we added mappings along the way
    if (newMappingsCreated) {
      jobStore.update(id, createCacheKey(), tempMailData);
    }
    return ImportResult.OK;
  }

  /**
   * Creates a label in the import account, if it doesn't already exist, for all labels associated
   * with the give {@code messages} .
   */
  private ImportResult importLabelsForMessages(
      UUID id,
      TokensAndUrlAuthData authData,
      TempMailData tempMailData,
      Supplier<Map<String, String>> allDestinationLabels,
      Collection<MailMessageModel> messages) {
    boolean newMappingsCreated = false;
    for (MailMessageModel mailMessageModel : messages) {
      // Get or create label ids associated with this message
      for (String exportedLabelName : mailMessageModel.getContainerIds()) {
        // Check if we have it in temp data
        String importerLabelId = tempMailData.getImportedId(exportedLabelName);
        if (importerLabelId == null) {
          // If a label with the same name already exists in the destination account, use that
          importerLabelId = allDestinationLabels.get().get(exportedLabelName);
          // Found no existing map or label named the same, create a new one
          if (importerLabelId == null) {
            try {
              importerLabelId = createImportedLabelId(authData, exportedLabelName);
            } catch (IOException e) {
              return new ImportResult(e);
            }
          }
          tempMailData.addFolderIdMapping(exportedLabelName, importerLabelId);
          newMappingsCreated = true;
        }
      }
    }

    // Persist temp data in case we added mappings along the way
    if (newMappingsCreated) {
      jobStore.update(id, createCacheKey(), tempMailData);
    }
    return ImportResult.OK;
  }

  /**
   * Import each message in {@code messages} into the import account with it's associated labels.
   */
  private ImportResult importMessages(
      TokensAndUrlAuthData authData,
      TempMailData tempMailData,
      Collection<MailMessageModel> messages) {
    for (MailMessageModel mailMessageModel : messages) {

      // Gather the label ids that will be associated with this message
      ImmutableList.Builder<String> importedLabelIds = ImmutableList.builder();
      for (String exportedLabelIdOrName : mailMessageModel.getContainerIds()) {
        // By this time all the label ids have been added to tempdata
        String importedLabelId = tempMailData.getImportedId(exportedLabelIdOrName);
        if (importedLabelId != null) {
          importedLabelIds.add(exportedLabelIdOrName);
        } else {
          logger.warn(
              "labels should have been added prior to importing messages"); // TODO remove after
          // testing
        }
        // Always add the migrated id
        importedLabelIds.add(tempMailData.getImportedId(LABEL));
      }
      // Create the message to import
      Message newMessage =
          new Message()
              .setRaw(mailMessageModel.getRawString())
              .setLabelIds(importedLabelIds.build());
      try {
        getOrCreateGmail(authData).users().messages().insert(USER, newMessage).execute();
      } catch (IOException e) {
        return new ImportResult(e);
      }
    }
    return new ImportResult(ResultType.OK);
  }

  private TempMailData getOrCreateMailData(UUID id) throws IOException {
    TempMailData tempMailData = jobStore.findData(id, createCacheKey(), TempMailData.class);
    if (tempMailData == null) {
      tempMailData = new TempMailData(id.toString());
      jobStore.create(id, createCacheKey(), tempMailData);
    }
    return tempMailData;
  }

  /** Supplies a mapping of Label Name -> Label Id (in the import account). */
  private Supplier<Map<String, String>> allDestinationLabelsSupplier(
      TokensAndUrlAuthData authData) {
    return new Supplier<Map<String, String>>() {
      @Override
      public Map<String, String> get() {
        ListLabelsResponse response = null;
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
      }
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

  /** Key for cache of album mappings.
   * TODO: Add a method parameter for a {@code key} for fine grained objects.
   */
  private String createCacheKey() {
    // TODO: store objects containing individual mappings instead of single object containing all mappings
    return "tempMailData";
  }
}
