package org.datatransferproject.datatransfer.google.drive;

import com.google.api.client.http.InputStreamContent;
import com.google.api.client.util.DateTime;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.datatransfer.google.common.GoogleCredentialFactory;
import org.datatransferproject.spi.cloud.storage.JobStore;
import org.datatransferproject.spi.transfer.provider.ImportResult;
import org.datatransferproject.spi.transfer.provider.Importer;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;
import org.datatransferproject.types.transfer.models.blob.BlobbyStorageContainerResource;
import org.datatransferproject.types.transfer.models.blob.DigitalDocumentWrapper;
import org.datatransferproject.types.transfer.models.blob.DtpDigitalDocument;

import java.io.IOException;
import java.util.UUID;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

/**
 * An {@link Importer} to export data from Google Drive.
 */
public final class DriveImporter implements
    Importer<TokensAndUrlAuthData, BlobbyStorageContainerResource> {

  private final GoogleCredentialFactory credentialFactory;
  private final JobStore jobStore;
  private final Monitor monitor;

  // Don't access this directly, instead access via getDriveInterface.
  private Drive driveInterface;

  public DriveImporter(GoogleCredentialFactory credentialFactory, JobStore jobStore, Monitor monitor) {
    this.credentialFactory = credentialFactory;
    this.jobStore = checkNotNull(jobStore, "Job store can't be null");
    this.monitor = monitor;
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
      String finalParentId = parentId;
      monitor.debug(()->format("Got parent id %s for old Id %s named: %s", finalParentId, data.getId(), data.getName()));
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
      for (DigitalDocumentWrapper file : data.getFiles()) {
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
      DigitalDocumentWrapper file,
      String parentId)
      throws IOException {
    InputStreamContent content = new InputStreamContent(
        null,
        jobStore.getStream(jobId, file.getCachedContentId()));
    DtpDigitalDocument dtpDigitalDocument = file.getDtpDigitalDocument();
    File driveFile = new File().setName(dtpDigitalDocument.getName());
    if (!Strings.isNullOrEmpty(parentId)) {
      driveFile.setParents(ImmutableList.of(parentId));
    }
    if (!Strings.isNullOrEmpty(dtpDigitalDocument.getDateModified())) {
      driveFile.setModifiedTime(DateTime.parseRfc3339(dtpDigitalDocument.getDateModified()));
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
      driveInterface = DriveExporter.makeDriveInterface(authData, credentialFactory);
    }

    return driveInterface;
  }
}
