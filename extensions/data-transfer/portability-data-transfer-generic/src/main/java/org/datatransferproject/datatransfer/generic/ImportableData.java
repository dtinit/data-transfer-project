package org.datatransferproject.datatransfer.generic;

import com.fasterxml.jackson.databind.JsonNode;

public class ImportableData {
  /** JSON data to POST */
  private JsonNode jsonData;

  /** Globally unique ID to avoid re-importing data */
  private String idempotentId;

  /** Human-readable item name */
  private String name;

  public ImportableData(JsonNode jsonData, String idempotentId, String name) {
    this.jsonData = jsonData;
    this.idempotentId = idempotentId;
    this.name = name;
  }

  public JsonNode getJsonData() {
    return jsonData;
  }

  public String getIdempotentId() {
    return idempotentId;
  }

  public String getName() {
    return name;
  }
}
