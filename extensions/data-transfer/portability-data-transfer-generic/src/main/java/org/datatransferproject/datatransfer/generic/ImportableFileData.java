package org.datatransferproject.datatransfer.generic;

import com.fasterxml.jackson.databind.JsonNode;
import org.datatransferproject.types.common.DownloadableItem;

public class ImportableFileData extends ImportableData {
  /** File file to POST * */
  private DownloadableItem file;

  public ImportableFileData(
      DownloadableItem file, JsonNode jsonData, String idempotentId, String name) {
    super(jsonData, idempotentId, name);
    this.file = file;
  }

  public DownloadableItem getFile() {
    return file;
  }
}
