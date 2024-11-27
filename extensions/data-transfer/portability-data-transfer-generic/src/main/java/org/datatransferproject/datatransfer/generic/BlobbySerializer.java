package org.datatransferproject.datatransfer.generic;

import static java.lang.String.format;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import org.datatransferproject.types.common.models.blob.BlobbyStorageContainerResource;
import org.datatransferproject.types.common.models.blob.DigitalDocumentWrapper;
import org.datatransferproject.types.common.models.blob.DtpDigitalDocument;

class BlobbyContainerPath {
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

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY)
class BlobbyFile {
  private final String folder;
  private final DtpDigitalDocument document;

  @JsonCreator
  public BlobbyFile(@JsonProperty String folder, @JsonProperty DtpDigitalDocument document) {
    this.folder = folder;
    this.document = document;
  }

  public String getFolder() {
    return folder;
  }

  public DtpDigitalDocument getDocument() {
    return document;
  }
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY)
class BlobbyFolder {
  private final String path;

  @JsonCreator
  public BlobbyFolder(@JsonProperty String path) {
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

  public static Iterable<ImportableData> serialize(
      BlobbyStorageContainerResource root, ObjectMapper objectMapper) {
    List<ImportableData> results = new ArrayList<>();
    // Search whole tree of container resource
    Queue<BlobbyContainerPath> horizon = new ArrayDeque<>();
    BlobbyContainerPath containerAndPath = new BlobbyContainerPath(root, "");
    do {
      BlobbyStorageContainerResource container = containerAndPath.getContainer();
      String parentPath = containerAndPath.getPath();
      String path = format("%s/%s", parentPath, container.getName());
      // Import the current folder
      GenericPayload<BlobbyFolder> blobbyFolder =
          new GenericPayload<>(new BlobbyFolder(path), SCHEMA_SOURCE);
      results.add(
          new ImportableData(objectMapper.valueToTree(blobbyFolder), container.getId(), path));

      // Add all sub-folders to the search tree
      for (BlobbyStorageContainerResource child : container.getFolders()) {
        horizon.add(new BlobbyContainerPath(child, path));
      }

      // Import all files in the current folder
      // Intentionally done after importing the current folder
      for (DigitalDocumentWrapper file : container.getFiles()) {
        GenericPayload<BlobbyFile> blobbyFile =
            new GenericPayload<>(new BlobbyFile(path, file.getDtpDigitalDocument()), SCHEMA_SOURCE);
        results.add(
            new ImportableFileData(
                new CachedDownloadableItem(
                    file.getCachedContentId(), file.getDtpDigitalDocument().getName()),
                objectMapper.valueToTree(blobbyFile),
                file.getCachedContentId(),
                file.getDtpDigitalDocument().getName()));
      }
    } while ((containerAndPath = horizon.poll()) != null);

    return results;
  }
}
