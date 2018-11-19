package org.datatransferproject.datatransfer.google.drive;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.http.InputStreamContent;
import com.google.api.client.util.DateTime;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.UUID;
import org.datatransferproject.datatransfer.google.common.GoogleCredentialFactory;
import org.datatransferproject.datatransfer.google.common.GoogleStaticObjects;
import org.datatransferproject.spi.cloud.storage.JobStore;
import org.datatransferproject.spi.transfer.provider.ImportResult;
import org.datatransferproject.spi.transfer.provider.Importer;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;
import org.datatransferproject.types.transfer.models.blob.BlobbyStorageContainerResource;
import org.datatransferproject.types.transfer.models.blob.DigitalDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DriveImporter implements
    Importer<TokensAndUrlAuthData, BlobbyStorageContainerResource> {
  private static final Logger logger = LoggerFactory.getLogger(DriveExporter.class);

  private final GoogleCredentialFactory credentialFactory;
  private volatile Drive driveInterface;
  private final JobStore jobStore;

  public DriveImporter(GoogleCredentialFactory credentialFactory, JobStore jobStore) {
    this(credentialFactory,
        // Lazily initialized later on
        null,
        jobStore);
  }

  @VisibleForTesting
  DriveImporter(
      GoogleCredentialFactory credentialFactory,
      Drive driveInterface,
      JobStore jobStore) {
    this.credentialFactory = credentialFactory;
    this.driveInterface = driveInterface;
    this.jobStore = checkNotNull(jobStore, "Job store can't be null");
  }

  @Override
  public ImportResult importItem(UUID jobId, TokensAndUrlAuthData authData,
      BlobbyStorageContainerResource data) throws Exception {
    String parentId = null;
    Drive driveInterface = getDriveInterface(authData);

    // Let the parent ID be empty for the root level
    if (Strings.isNullOrEmpty(data.getId()) || "root".equals(data.getId())) {
      parentId = importSingleFolder(
          jobId,
          driveInterface,
          "MigratedContent",
          "root",
          null);
    } else {
      DriveFolderMapping mapping = jobStore.findData(jobId, data.getId(), DriveFolderMapping.class);
      checkNotNull(mapping, "No mapping found for %s", data.getId());
      logger.info("Got parent id {} for old Id {} named: {}", parentId, data.getId(), data.getName());
      parentId = mapping.getNewId();
    }

    // Uploads album metadata
    if (data.getFolders() != null && data.getFolders().size() > 0) {
      for (BlobbyStorageContainerResource folder : data.getFolders()) {
        importSingleFolder(jobId, driveInterface, folder.getName(), folder.getId(), parentId);
      }
    }

    // Uploads photos
    if (data.getFiles() != null && data.getFiles().size() > 0) {
      for (DigitalDocument file : data.getFiles()) {
        importSingleFile(jobId, driveInterface, file, parentId);
      }
    }

    return ImportResult.OK;
  }

  private String importSingleFolder(
      UUID jobId,
      Drive driveInterface,
      String folderName,
      String folderId,
      String parentId) throws IOException {
    File newFolder = new File()
        .setName(folderName)
        .setMimeType(DriveExporter.FOLDER_MIME_TYPE);
    if (!Strings.isNullOrEmpty(parentId)) {
      newFolder.setParents(ImmutableList.of(parentId));
    }
    File resultFolder = driveInterface.files().create(newFolder).execute();
    DriveFolderMapping mapping = new DriveFolderMapping(folderId, resultFolder.getId());
    jobStore.update(jobId, folderId, mapping);
    return resultFolder.getId();
  }

  private void importSingleFile(
      UUID jobId,
      Drive driveInterface,
      DigitalDocument file,
      String parentId)
      throws IOException {
    InputStreamContent content = new InputStreamContent(
        null,
        jobStore.getStream(jobId, file.getCachedContentId()));
    File driveFile = new File().setName(file.getName());
    if (!Strings.isNullOrEmpty(parentId)) {
      driveFile.setParents(ImmutableList.of(parentId));
    }
    if (!Strings.isNullOrEmpty(file.getDateModified())) {
      driveFile.setModifiedTime(DateTime.parseRfc3339(file.getDateModified()));
    }
    if (!Strings.isNullOrEmpty(file.getOriginalEncodingFormat())
        && file.getOriginalEncodingFormat().startsWith("application/vnd.google-apps.")) {
      driveFile.setMimeType(file.getOriginalEncodingFormat());
    }
    driveInterface.files().create(
        driveFile,
        content
    ).execute();
  }

  private synchronized Drive getDriveInterface(TokensAndUrlAuthData authData) {
    if (driveInterface == null) {
      driveInterface = makeDriveInterface(authData);
    }

    return driveInterface;
  }

  private synchronized Drive makeDriveInterface(TokensAndUrlAuthData authData) {
    Credential credential = credentialFactory.createCredential(authData);
    return new Drive.Builder(
        credentialFactory.getHttpTransport(), credentialFactory.getJsonFactory(), credential)
        .setApplicationName(GoogleStaticObjects.APP_NAME)
        .build();
  }
}
