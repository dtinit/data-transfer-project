package org.datatransferproject.datatransfer.generic;

import static java.lang.String.format;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.datatransferproject.spi.cloud.storage.TemporaryPerJobDataStore;
import org.datatransferproject.transfer.JobMetadata;
import org.datatransferproject.types.common.DownloadableItem;
import org.datatransferproject.types.common.models.blob.BlobbyStorageContainerResource;
import org.datatransferproject.types.common.models.blob.DigitalDocumentWrapper;
import org.datatransferproject.types.common.models.blob.DtpDigitalDocument;

/**
 * Wrapper to adapt items known to be in temp storage (e.g. BLOB data) into {@link DownloadableItem}
 *
 * <p>It's useful to wrap such items so upstream code can consume either known temp store'd items or
 * items the Importer has to download itself (some MEDIA items) from the same interface.
 */
class CachedDownloadableItem implements DownloadableItem {
  private final String cachedId;
  private final String name;

  public CachedDownloadableItem(String cachedId, String name) {
    this.cachedId = cachedId;
    this.name = name;
  }

  @Override
  public String getIdempotentId() {
    return cachedId;
  }

  @Override
  public String getFetchableUrl() {
    // 'url' is ID when cached
    return cachedId;
  }

  @Override
  public boolean isInTempStore() {
    return true;
  }

  @Override
  public String getName() {
    return name;
  }
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
@JsonTypeName("File")
class FileExportData implements BlobbySerializer.ExportData {
  @JsonProperty private final String folder;
  @JsonProperty private final String name;
  @JsonProperty private final Optional<ZonedDateTime> dateModified;

  private FileExportData(String folder, String name, Optional<ZonedDateTime> dateModified) {
    this.folder = folder;
    this.name = name;
    this.dateModified = dateModified;
  }

  public static FileExportData fromDtpDigitalDocument(String path, DtpDigitalDocument model) {
    return new FileExportData(
        path,
        model.getName(),
        Optional.ofNullable(model.getDateModified())
            .filter(string -> !string.isEmpty())
            .map(dateString -> ZonedDateTime.parse(model.getDateModified())));
  }

  public String getFolder() {
    return folder;
  }

  public String getName() {
    return name;
  }

  public Optional<ZonedDateTime> getDateModified() {
    return dateModified;
  }
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
@JsonTypeName("Folder")
class FolderExportData implements BlobbySerializer.ExportData {
  @JsonProperty private final String path;

  @JsonCreator
  public FolderExportData(@JsonProperty String path) {
    this.path = path;
  }

  public String getPath() {
    return path;
  }
}

public class BlobbySerializer {

  static final String SCHEMA_SOURCE =
      GenericTransferConstants.SCHEMA_SOURCE_BASE
          + "/extensions/data-transfer/portability-data-transfer-generic/src/main/java/org/datatransferproject/datatransfer/generic/BlobbySerializer.java";
  private static final String BLOB_ID_TO_NAME_KEY = "blobIdToNameKey";
  private final TemporaryPerJobDataStore jobStore;
  private BlobIdToName blobIdToName;

  public BlobbySerializer(TemporaryPerJobDataStore jobStore) {
    this.jobStore = jobStore;
  }

  private void addToJobStore(String id, String name) throws IOException {
    initialiseBlobIdToNameIfNot(JobMetadata.getJobId());
    blobIdToName.add(id, name);
  }

  private void saveStateToStore() throws IOException {
    initialiseBlobIdToNameIfNot(JobMetadata.getJobId());

    jobStore.create(JobMetadata.getJobId(), BLOB_ID_TO_NAME_KEY, blobIdToName);
  }

  private String getFromStore(String id) throws IOException {
    initialiseBlobIdToNameIfNot(JobMetadata.getJobId());
    return blobIdToName.get(id);
  }

  private void initialiseBlobIdToNameIfNot(UUID jobId) throws IOException {
    if (blobIdToName == null) {

      blobIdToName = jobStore.findData(jobId, BLOB_ID_TO_NAME_KEY, BlobIdToName.class);

      if (blobIdToName == null) {
        blobIdToName = new BlobIdToName();
      }
    }
  }

  /**
   * Serializes a BlobbyStorageContainerResource into an iterable of ImportableData objects.
   *
   * <p>This method only serializes the tree up to a single depth, assuming that the exporter will
   * send separate BlobbyStorageContainerResource objects for subfolders. It also stores an
   * ID-to-path mapping for folders, which can be used to establish parent-child relationships in
   * separate iterations.
   *
   * @param root The BloppyStorageContainerResource to serialize.
   * @return An iterable of ImportableData objects representing the serialized data.
   */
  public Iterable<ImportableData<ExportData>> serialize(BlobbyStorageContainerResource root)
      throws IOException {
    List<ImportableData<ExportData>> results = new ArrayList<>();

    String currentFolderPath = getFromStore(root.getId());
    if (currentFolderPath == null) {
      currentFolderPath = "/" + root.getName();
    }

    // Import the current folder
    results.add(
        new ImportableData<>(
            new GenericPayload<>(new FolderExportData(currentFolderPath), SCHEMA_SOURCE),
            root.getId(),
            currentFolderPath));

    // Import all sub folders, not recursively
    for (BlobbyStorageContainerResource childFolder : root.getFolders()) {
      String path = format("%s/%s", currentFolderPath, childFolder.getName());
      results.add(
          new ImportableData<>(
              new GenericPayload<>(new FolderExportData(path), SCHEMA_SOURCE),
              childFolder.getId(),
              path));
      addToJobStore(childFolder.getId(), path);
    }

    // Import all files in the current folder
    // Intentionally done after importing the current folder
    for (DigitalDocumentWrapper file : root.getFiles()) {
      results.add(
          new ImportableFileData<>(
              new CachedDownloadableItem(
                  file.getCachedContentId(), file.getDtpDigitalDocument().getName()),
              file.getDtpDigitalDocument().getEncodingFormat(),
              new GenericPayload<>(
                  FileExportData.fromDtpDigitalDocument(
                      currentFolderPath, file.getDtpDigitalDocument()),
                  SCHEMA_SOURCE),
              file.getCachedContentId(),
              file.getDtpDigitalDocument().getName()));
    }

    saveStateToStore();
    return results;
  }

  @JsonSubTypes({
    @JsonSubTypes.Type(FolderExportData.class),
    @JsonSubTypes.Type(FileExportData.class),
  })
  public interface ExportData {}
}
