package org.datatransferproject.datatransfer.generic;

import static java.lang.String.format;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.time.ZonedDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
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
  private String cachedId;
  private String name;

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
@JsonTypeName("Document")
class DocumentExportData implements BlobbySerializer.ExportData {
  @JsonProperty private final String name;
  @JsonProperty private final Optional<ZonedDateTime> dateModified;

  private DocumentExportData(String name, Optional<ZonedDateTime> dateModified) {
    this.name = name;
    this.dateModified = dateModified;
  }

  public static DocumentExportData fromModel(DtpDigitalDocument model) {
    return new DocumentExportData(
        model.getName(),
        Optional.ofNullable(model.getDateModified())
            .filter(string -> !string.isEmpty())
            .map(dateString -> ZonedDateTime.parse(model.getDateModified())));
  }

  public String getName() {
    return name;
  }

  public Optional<ZonedDateTime> getDateModified() {
    return dateModified;
  }
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
@JsonTypeName("File")
class FileExportData implements BlobbySerializer.ExportData {
  @JsonProperty private final String folder;
  @JsonProperty private final DocumentExportData document;

  public FileExportData(String folder, DocumentExportData document) {
    this.folder = folder;
    this.document = document;
  }

  public String getFolder() {
    return folder;
  }

  public DocumentExportData getDocument() {
    return document;
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
  @JsonSubTypes({
    @JsonSubTypes.Type(FolderExportData.class),
    @JsonSubTypes.Type(FileExportData.class),
  })
  public interface ExportData {}

  static class BlobbyContainerPath {
    private BlobbyStorageContainerResource container;
    private String path;

    public BlobbyContainerPath(BlobbyStorageContainerResource container, String path) {
      this.container = container;
      this.path = path;
    }

    public BlobbyStorageContainerResource getContainer() {
      return container;
    }

    public String getPath() {
      return path;
    }
  }

  static final String SCHEMA_SOURCE =
      GenericTransferConstants.SCHEMA_SOURCE_BASE
          + "/extensions/data-transfer/portability-data-transfer-generic/src/main/java/org/datatransferproject/datatransfer/generic/BlobbySerializer.java";

  public static Iterable<ImportableData<ExportData>> serialize(
      BlobbyStorageContainerResource root) {
    List<ImportableData<ExportData>> results = new ArrayList<>();
    // Search whole tree of container resource
    Queue<BlobbyContainerPath> horizon = new ArrayDeque<>();
    BlobbyContainerPath containerAndPath = new BlobbyContainerPath(root, "");
    do {
      BlobbyStorageContainerResource container = containerAndPath.getContainer();
      String parentPath = containerAndPath.getPath();
      String path = format("%s/%s", parentPath, container.getName());
      // Import the current folder
      results.add(
          new ImportableData<>(
              new GenericPayload<>(new FolderExportData(path), SCHEMA_SOURCE),
              container.getId(),
              path));

      // Add all sub-folders to the search tree
      for (BlobbyStorageContainerResource child : container.getFolders()) {
        horizon.add(new BlobbyContainerPath(child, path));
      }

      // Import all files in the current folder
      // Intentionally done after importing the current folder
      for (DigitalDocumentWrapper file : container.getFiles()) {
        results.add(
            new ImportableFileData<>(
                new CachedDownloadableItem(
                    file.getCachedContentId(), file.getDtpDigitalDocument().getName()),
                new GenericPayload<>(
                    new FileExportData(
                        path, DocumentExportData.fromModel(file.getDtpDigitalDocument())),
                    SCHEMA_SOURCE),
                file.getCachedContentId(),
                file.getDtpDigitalDocument().getName()));
      }
    } while ((containerAndPath = horizon.poll()) != null);

    return results;
  }
}
