package org.datatransferproject.datatransfer.generic;

import java.util.HashMap;
import java.util.Map;
import org.datatransferproject.types.common.models.DataModel;

public class BlobIdToName extends DataModel {

  private Map<String, String> idToName;

  public BlobIdToName() {
    this.idToName = new HashMap<>();
  }

  public String get(String id) {
    return idToName.get(id);
  }

  public String add(String id, String name) {
    return idToName.put(id, name);
  }
}
