package org.datatransferproject.datatransfer.generic;

public class ImportableData<T> {
  /** JSON serializable data to POST */
  private GenericPayload<T> jsonData;

  /** Globally unique ID to avoid re-importing data */
  private String idempotentId;

  /** Human-readable item name */
  private String name;

  public ImportableData(GenericPayload<T> jsonData, String idempotentId, String name) {
    this.jsonData = jsonData;
    this.idempotentId = idempotentId;
    this.name = name;
  }

  public GenericPayload<T> getJsonData() {
    return jsonData;
  }

  public String getIdempotentId() {
    return idempotentId;
  }

  public String getName() {
    return name;
  }
}
