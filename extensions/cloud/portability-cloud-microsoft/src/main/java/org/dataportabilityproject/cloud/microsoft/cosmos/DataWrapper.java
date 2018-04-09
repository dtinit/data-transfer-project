package org.dataportabilityproject.cloud.microsoft.cosmos;

import com.microsoft.azure.storage.table.TableServiceEntity;

/** Wraps Job data for serialization to Cosmos DB */
public class DataWrapper extends TableServiceEntity {
  private String serialized;
  private String state;

  public DataWrapper(String partitionKey, String rowKey, String state, String serialized) {
    this.partitionKey = partitionKey;
    this.rowKey = rowKey;
    this.state = state;
    this.serialized = serialized;
  }

  /** Required by Azure Table Store API */
  public DataWrapper() {}

  public String getSerialized() {
    return serialized;
  }

  public void setSerialized(String serialized) {
    this.serialized = serialized;
  }

  public String getState() {
    return state;
  }

  public void setState(String state) {
    this.state = state;
  }
}
