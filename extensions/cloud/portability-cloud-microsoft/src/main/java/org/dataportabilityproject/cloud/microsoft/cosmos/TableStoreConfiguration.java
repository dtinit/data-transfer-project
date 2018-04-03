package org.dataportabilityproject.cloud.microsoft.cosmos;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;

/** Configuration required to initialize the Azure job store */
class TableStoreConfiguration {
  private String accountName;
  private String accountKey;
  private String blobKey;
  private String partitionKey;
  private ObjectMapper mapper;

  public String getAccountName() {
    return accountName;
  }

  public String getAccountKey() {
    return accountKey;
  }

  public String getPartitionKey() {
    return partitionKey;
  }

  public String getBlobKey() {
    return blobKey;
  }

  public ObjectMapper getMapper() {
    return mapper;
  }

  public static class Builder {
    private final TableStoreConfiguration configuration;

    public static Builder newInstance() {
      return new Builder();
    }

    public Builder accountName(String accountName) {
      configuration.accountName = accountName;
      return this;
    }

    public Builder accountKey(String accountKey) {
      configuration.accountKey = accountKey;
      return this;
    }

    public Builder partitionKey(String partitionKey) {
      configuration.partitionKey = partitionKey;
      return this;
    }

    public Builder blobKey(String blobKey) {
      configuration.blobKey = blobKey;
      return this;
    }

    public Builder mapper(ObjectMapper mapper) {
      configuration.mapper = mapper;
      return this;
    }

    public TableStoreConfiguration build() {
      Preconditions.checkNotNull(configuration.accountName);
      Preconditions.checkNotNull(configuration.accountKey);
      Preconditions.checkNotNull(configuration.partitionKey);
      Preconditions.checkNotNull(configuration.blobKey);
      Preconditions.checkNotNull(configuration.mapper);
      return configuration;
    }

    private Builder() {
      configuration = new TableStoreConfiguration();
    }
  }
}
