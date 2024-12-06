package org.datatransferproject.datatransfer.generic;

import org.datatransferproject.types.common.DownloadableItem;

public class ImportableFileData<T> extends ImportableData<T> {
  /** File file to POST * */
  private DownloadableItem file;

  private String fileMimeType;

  public ImportableFileData(
      DownloadableItem file,
      String fileMimeType,
      GenericPayload<T> jsonData,
      String idempotentId,
      String name) {
    super(jsonData, idempotentId, name);
    this.file = file;
    this.fileMimeType = fileMimeType;
  }

  public DownloadableItem getFile() {
    return file;
  }

  public String getFileMimeType() {
    return fileMimeType;
  }
}
