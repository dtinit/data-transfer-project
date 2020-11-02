package org.datatransferproject.datatransfer.google.drive;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.api.client.http.InputStreamContent;
import com.google.api.client.util.DateTime;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.UUID;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.datatransfer.google.common.GoogleCredentialFactory;
import org.datatransferproject.spi.cloud.storage.TemporaryPerJobDataStore;
import org.datatransferproject.spi.transfer.idempotentexecutor.IdempotentImportExecutor;
import org.datatransferproject.spi.transfer.provider.ImportResult;
import org.datatransferproject.spi.transfer.provider.Importer;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;
import org.datatransferproject.types.transfer.models.blob.BlobbyStorageContainerResource;
import org.datatransferproject.types.transfer.models.blob.DigitalDocumentWrapper;
import org.datatransferproject.types.transfer.models.blob.DtpDigitalDocument;

/** An {@link Importer} to import data to Google Drive. */
public final class DriveImporter
    implements Importer<TokensAndUrlAuthData, BlobbyStorageContainerResource> {
  private static final String ROOT_FOLDER_ID = "root-id";

  private final GoogleCredentialFactory credentialFactory;
  private final TemporaryPerJobDataStore jobStore;
  private final Monitor monitor;

  // Don't access this directly, instead access via getDriveInterface.
  private Drive driveInterface;

  public DriveImporter(
      GoogleCredentialFactory credentialFactory,
      TemporaryPerJobDataStore jobStore,
      Monitor monitor) {
    this.credentialFactory = credentialFactory;
    this.jobStore = checkNotNull(jobStore, "Job store can't be null");
    this.monitor = monitor;
  }

  @Override
  public ImportResult importItem(
      UUID jobId,
      IdempotentImportExecutor idempotentExecutor,
      TokensAndUrlAuthData authData,
      BlobbyStorageContainerResource data)
      throws Exception {
    String parentId;
    Drive driveInterface = getDriveInterface(authData);

    // Let the parent ID be empty for the root level
    if (Strings.isNullOrEmpty(data.getId()) || "root".equals(data.getId())) {
      parentId =
          idempotentExecutor.executeOrThrowException(
              ROOT_FOLDER_ID,
              data.getName(),
              () -> importSingleFolder(driveInterface, "MigratedContent", null));
    } else {
      parentId = idempotentExecutor.getCachedValue(ROOT_FOLDER_ID);
    }

    // Uploads album metadata
    if (data.getFolders() != null && data.getFolders().size() > 0) {
      for (BlobbyStorageContainerResource folder : data.getFolders()) {
        idempotentExecutor.executeAndSwallowIOExceptions(
            folder.getId(),
            folder.getName(),
            () -> importSingleFolder(driveInterface, folder.getName(), parentId));
      }
    }

    // Uploads photos
    if (data.getFiles() != null && data.getFiles().size() > 0) {
      for (DigitalDocumentWrapper file : data.getFiles()) {
        idempotentExecutor.executeAndSwallowIOExceptions(
            Integer.toString(file.hashCode()),
            file.getDtpDigitalDocument().getName(),
            () -> importSingleFile(jobId, driveInterface, file, parentId));
      }
    }

    return ImportResult.OK;
  }

  private String importSingleFolder(Drive driveInterface, String folderName, String parentId)
      throws IOException {
    File newFolder = new File().setName(folderName).setMimeType(DriveExporter.FOLDER_MIME_TYPE);
    if (!Strings.isNullOrEmpty(parentId)) {
      newFolder.setParents(ImmutableList.of(parentId));
    }
    File resultFolder = driveInterface.files().create(newFolder).execute();
    return resultFolder.getId();
  }

  private String importSingleFile(
      UUID jobId, Drive driveInterface, DigitalDocumentWrapper file, String parentId)
      throws IOException {
    InputStreamContent content =
        new InputStreamContent(
            null, jobStore.getStream(jobId, file.getCachedContentId()).getStream());
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
    return driveInterface.files().create(driveFile, content).execute().getId();
  }

  private synchronized Drive getDriveInterface(TokensAndUrlAuthData authData) {
    if (driveInterface == null) {
      driveInterface = DriveExporter.makeDriveInterface(authData, credentialFactory);
    }

    return driveInterface;
  }
}
