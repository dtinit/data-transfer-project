package org.datatransferproject.datatransfer.google.drive;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.Drive.Files.List;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.datatransfer.google.common.GoogleCredentialFactory;
import org.datatransferproject.datatransfer.google.common.GoogleStaticObjects;
import org.datatransferproject.spi.cloud.storage.TemporaryPerJobDataStore;
import org.datatransferproject.spi.transfer.provider.ExportResult;
import org.datatransferproject.spi.transfer.provider.ExportResult.ResultType;
import org.datatransferproject.spi.transfer.provider.Exporter;
import org.datatransferproject.spi.transfer.types.ContinuationData;
import org.datatransferproject.types.common.ExportInformation;
import org.datatransferproject.types.common.StringPaginationToken;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;
import org.datatransferproject.types.transfer.models.blob.BlobbyStorageContainerResource;
import org.datatransferproject.types.transfer.models.blob.DigitalDocumentWrapper;
import org.datatransferproject.types.transfer.models.blob.DtpDigitalDocument;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * An {@link Exporter} to export data from Google Drive.
 *
 * <p>Exports first part content in MS format, and blobby content in the original format.
 *
 * <p>Doesn't necessarily export all files in Drive, things like Maps, and Fusion Tables are
 * currently skipped as there isn't a good export mechanism for them.
 */
public final class DriveExporter
    implements Exporter<TokensAndUrlAuthData, BlobbyStorageContainerResource> {
  private static final String DRIVE_QUERY_FORMAT = "'%s' in parents and trashed=false";
  static final String FOLDER_MIME_TYPE = "application/vnd.google-apps.folder";
  private static final String FUSION_TABLE_MIME_TYPE = "application/vnd.google-apps.fusiontable";
  private static final String MAP_MIME_TYPE = "application/vnd.google-apps.map";
  // This is generated from calling https://www.googleapis.com/drive/v3/about and
  // picking the best default.
  private static final ImmutableMap<String, String> EXPORT_FORMATS =
      ImmutableMap.<String, String>builder()
          .put(
              "application/vnd.google-apps.document",
              "application/vnd.openxmlformats-officedocument.wordprocessingml.document")
          .put(
              "application/vnd.google-apps.spreadsheet",
              "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
          .put("application/vnd.google-apps.drawing", "image/svg+xml")
          .put(
              "application/vnd.google-apps.presentation",
              "application/vnd.openxmlformats-officedocument.presentationml.presentation")
          .put("application/vnd.google-apps.script", "application/vnd.google-apps.script+json")
          .put("application/vnd.google-apps.jam", "application/pdf")
          .put("application/vnd.google-apps.form", "application/zip")
          .put("application/vnd.google-apps.site", "text/plain")
          .build();

  private final GoogleCredentialFactory credentialFactory;
  private final TemporaryPerJobDataStore jobStore;
  private final Monitor monitor;

  // Don't access this directly, instead access via getDriveInterface.
  private Drive driveInterface;

  public DriveExporter(
      GoogleCredentialFactory credentialFactory,
      TemporaryPerJobDataStore jobStore,
      Monitor monitor) {
    this.credentialFactory = checkNotNull(credentialFactory, "Credential Factory can't be null");
    this.jobStore = checkNotNull(jobStore, "Job store can't be null");
    this.monitor = monitor;
  }

  @Override
  public ExportResult<BlobbyStorageContainerResource> export(
      UUID jobId,
      TokensAndUrlAuthData authData,
      Optional<ExportInformation> optionalExportInformation)
      throws Exception {
    Drive driveInterface = getDriveInterface((authData));
    List driveListOperation = driveInterface.files().list();
    // If the folder Id isn't specified then use root
    String parentId = "root";
    if (optionalExportInformation.isPresent()) {
      ExportInformation exportInformation = optionalExportInformation.get();
      if (exportInformation.getPaginationData() != null) {
        StringPaginationToken paginationToken =
            (StringPaginationToken) exportInformation.getPaginationData();
        driveListOperation.setPageToken(paginationToken.getToken());
      }

      if (exportInformation.getContainerResource() != null) {
        BlobbyStorageContainerResource parent =
            (BlobbyStorageContainerResource) exportInformation.getContainerResource();
        parentId = parent.getId();
      }
    }
    driveListOperation
        .setFields("files(id, name, modifiedTime, mimeType)")
        .setQ(String.format(DRIVE_QUERY_FORMAT, parentId));

    ArrayList<DigitalDocumentWrapper> files = new ArrayList<>();
    ArrayList<BlobbyStorageContainerResource> folders = new ArrayList<>();

    FileList fileList = driveListOperation.execute();

    for (File file : fileList.getFiles()) {
      if (FOLDER_MIME_TYPE.equals(file.getMimeType())) {
        folders.add(new BlobbyStorageContainerResource(file.getName(), file.getId(), null, null));
      } else if (FUSION_TABLE_MIME_TYPE.equals(file.getMimeType())) {
        monitor.info(() -> "Exporting of fusion tables is not yet supported: " + file);
      } else if (MAP_MIME_TYPE.equals(file.getMimeType())) {
        monitor.info(() -> "Exporting of maps is not yet supported: " + file);
      } else {
        try {
          InputStream inputStream;
          String newMimeType = file.getMimeType();
          if (EXPORT_FORMATS.containsKey(file.getMimeType())) {
            newMimeType = EXPORT_FORMATS.get(file.getMimeType());
            inputStream =
                driveInterface
                    .files()
                    .export(file.getId(), newMimeType)
                    .executeMedia()
                    .getContent();
          } else {
            inputStream =
                driveInterface
                    .files()
                    .get(file.getId())
                    .setAlt("media")
                    .executeMedia()
                    .getContent();
          }
          jobStore.create(jobId, file.getId(), inputStream);
          files.add(
              new DigitalDocumentWrapper(
                  new DtpDigitalDocument(
                      file.getName(), file.getModifiedTime().toStringRfc3339(), newMimeType),
                  file.getMimeType(),
                  file.getId()));
        } catch (Exception e) {
          monitor.severe(() -> "Error exporting " + file, e);
        }
      }
      monitor.info(() -> "Exported " + file);
    }

    ResultType resultType = isDone(fileList) ? ResultType.END : ResultType.CONTINUE;

    BlobbyStorageContainerResource result =
        new BlobbyStorageContainerResource(null, parentId, files, folders);

    StringPaginationToken paginationToken = null;
    if (!Strings.isNullOrEmpty(fileList.getNextPageToken())) {
      paginationToken = new StringPaginationToken(fileList.getNextPageToken());
    }

    ContinuationData continuationData = new ContinuationData(paginationToken);
    folders.forEach(continuationData::addContainerResource);
    return new ExportResult<>(resultType, result, continuationData);
  }

  private static boolean isDone(FileList fileList) {
    return fileList.getFiles().isEmpty() || Strings.isNullOrEmpty(fileList.getNextPageToken());
  }

  private synchronized Drive getDriveInterface(TokensAndUrlAuthData authData) {
    if (driveInterface == null) {
      driveInterface = makeDriveInterface(authData, credentialFactory);
    }

    return driveInterface;
  }

  static synchronized Drive makeDriveInterface(
      TokensAndUrlAuthData authData, GoogleCredentialFactory credentialFactory) {
    Credential credential = credentialFactory.createCredential(authData);
    return new Drive.Builder(
            credentialFactory.getHttpTransport(), credentialFactory.getJsonFactory(), credential)
        .setApplicationName(GoogleStaticObjects.APP_NAME)
        .build();
  }
}
