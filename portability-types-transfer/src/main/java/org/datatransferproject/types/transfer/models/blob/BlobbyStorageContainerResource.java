package org.datatransferproject.types.transfer.models.blob;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import org.datatransferproject.types.common.models.ContainerResource;

import java.util.Collection;

/**
 * Represents a folder in a blobby storage system such as Google Drive or Microsoft OneDrive.
 * Can contain multiple files (via {@link DigitalDocumentWrapper}) or folders (via nested
 * BlobbyStorageContainerResource).
 */
public class BlobbyStorageContainerResource extends ContainerResource {

  private final String name;
  private final String id;
  private final Collection<DigitalDocumentWrapper> files;
  private final Collection<BlobbyStorageContainerResource> folders;

  @JsonCreator
  public BlobbyStorageContainerResource(
      @JsonProperty("name") String name,
      @JsonProperty("id") String id,
      @JsonProperty("files") Collection<DigitalDocumentWrapper> files,
      @JsonProperty("folders") Collection<BlobbyStorageContainerResource> folders) {
    this.name = name;
    this.id = id;
    this.files = files == null ? ImmutableList.of() : files;
    this.folders = folders == null ? ImmutableList.of() : folders;
  }

  public Collection<DigitalDocumentWrapper> getFiles() {
    return files;
  }

  public Collection<BlobbyStorageContainerResource> getFolders() {
    return folders;
  }

  public String getName() {
    return name;
  }

  public String getId() {
    return id;
  }
}
