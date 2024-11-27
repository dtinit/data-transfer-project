package org.datatransferproject.datatransfer.generic;

import org.datatransferproject.types.common.DownloadableItem;

public class ImportableFileData<T> extends ImportableData<T> {
  /** File file to POST * */
  private DownloadableItem file;

  public ImportableFileData(
      DownloadableItem file, GenericPayload<T> jsonData, String idempotentId, String name) {
    super(jsonData, idempotentId, name);
    this.file = file;
  }

  public DownloadableItem getFile() {
    return file;
  }
}
