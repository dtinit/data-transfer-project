package org.datatransferproject.cloud.microsoft.cosmos;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import com.microsoft.azure.storage.table.CloudTable;
import com.microsoft.azure.storage.table.CloudTableClient;
import com.microsoft.azure.storage.table.TableOperation;
import com.microsoft.azure.storage.table.TableQuery;
import com.microsoft.azure.storage.table.TableResult;
import org.datatransferproject.spi.cloud.storage.JobStoreWithValidator;
import org.datatransferproject.spi.cloud.types.JobAuthorization;
import org.datatransferproject.spi.cloud.types.PortabilityJob;
import org.datatransferproject.types.common.models.DataModel;
import org.datatransferproject.types.transfer.errors.ErrorDetail;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.util.Collection;
import java.util.Iterator;
import java.util.UUID;

import static com.microsoft.azure.storage.table.TableQuery.generateFilterCondition;

/** Uses the Azure Cosmos DB Table Storage API to persist job data. */
public class AzureTableStore extends JobStoreWithValidator {
  private static final String COSMOS_CONNECTION_TEMPLATE =
      "DefaultEndpointsProtocol=https;AccountName=%s;AccountKey=%s;TableEndpoint=%s;";
  private static final String ENDPOINT_TEMPLATE = "https://%s.table.cosmosdb.azure.com:443/";
  private static final String BLOB_CONNECTION_TEMPLATE =
      "DefaultEndpointsProtocol=https;AccountName=%s;AccountKey=%s;";

  private static final String JOB_TABLE = "DP_JOBS";
  private static final String JOB_DATA_TABLE = "DP_JOB_DATA";

  private static final String BLOB_CONTAINER =
      "dataportability"; // Azure rules: The container name must be lowercase
  private static final int UNKNOWN_LENGTH = -1;

  private final TableStoreConfiguration configuration;

  private CloudTableClient tableClient;
  private CloudBlobClient blobClient;

  public AzureTableStore(TableStoreConfiguration configuration) {
    this.configuration = configuration;
  }

  public void init() {
    try {
      String endpoint = String.format(ENDPOINT_TEMPLATE, configuration.getAccountName());

      CloudStorageAccount cosmosAccount =
          CloudStorageAccount.parse(
              String.format(
                  COSMOS_CONNECTION_TEMPLATE,
                  configuration.getAccountName(),
                  configuration.getAccountKey(),
                  endpoint));
      tableClient = cosmosAccount.createCloudTableClient();

      // Create the tables if the do not exist
      tableClient.getTableReference(JOB_TABLE).createIfNotExists();
      tableClient.getTableReference(JOB_DATA_TABLE).createIfNotExists();

      CloudStorageAccount blobAccount =
          CloudStorageAccount.parse(
              String.format(
                  BLOB_CONNECTION_TEMPLATE,
                  configuration.getAccountName(),
                  configuration.getBlobKey()));
      blobClient = blobAccount.createCloudBlobClient();

      blobClient.getContainerReference(BLOB_CONTAINER).createIfNotExists();
    } catch (StorageException | URISyntaxException | InvalidKeyException e) {
      throw new MicrosoftStorageException(e);
    }
  }

  @Override
  public void createJob(UUID jobId, PortabilityJob job) throws IOException {

    Preconditions.checkNotNull(jobId, "Job id is null");

    Preconditions.checkNotNull(job, "Job is null");

    try {

      create(jobId.toString(), JOB_TABLE, job.jobAuthorization().state().name(), job);

    } catch (JsonProcessingException e) {
      throw new IOException("Error creating job: " + jobId, e);
    }
  }

  @Override
  public void updateJob(UUID jobId, PortabilityJob job) throws IOException {
    updateJob(jobId, job, null);
  }

  @Override
  protected void updateJob(UUID jobId, PortabilityJob job, JobUpdateValidator validator)
      throws IOException {

    Preconditions.checkNotNull(jobId, "Job is null");

    Preconditions.checkNotNull(job, "Job is null");

    try {

      CloudTable table = tableClient.getTableReference(JOB_TABLE);

      String serializedJob = configuration.getMapper().writeValueAsString(job);
      DataWrapper wrapper =
          new DataWrapper(
              configuration.getPartitionKey(),
              jobId.toString(),
              job.jobAuthorization().state().name(),
              serializedJob);

      if (validator != null) {
        PortabilityJob previousJob = findJob(jobId);
        if (previousJob == null) {
          throw new IOException("Could not find record for jobId: " + jobId);
        }

        validator.validate(previousJob, job);
      }

      TableOperation insert = TableOperation.insertOrReplace(wrapper);
      table.execute(insert);

    } catch (JsonProcessingException | StorageException | URISyntaxException e) {
      throw new IOException("Error updating job: " + jobId, e);
    }
  }

  @Override
  public void addErrorsToJob(UUID jobId, Collection<ErrorDetail> errors) throws IOException {
    // TODO(jimmarino): Implement an error storage solution for Azure.
    throw new IOException("Adding errors not yet implemented for Azure");
  }

  @Override
  public PortabilityJob findJob(UUID jobId) {
    Preconditions.checkNotNull(jobId, "Job id is null");
    try {

      CloudTable table = tableClient.getTableReference(JOB_TABLE);

      TableOperation retrieve =
          TableOperation.retrieve(
              configuration.getPartitionKey(), jobId.toString(), DataWrapper.class);
      TableResult result = table.execute(retrieve);
      DataWrapper wrapper = result.getResultAsType();
      return configuration.getMapper().readValue(wrapper.getSerialized(), PortabilityJob.class);
    } catch (StorageException | URISyntaxException | IOException e) {
      throw new MicrosoftStorageException("Error finding job: " + jobId, e);
    }
  }

  @Override
  public void remove(UUID jobId) throws IOException {
    Preconditions.checkNotNull(jobId, "Job id is null");
    remove(jobId, JOB_TABLE);
  }

  @Override
  public <T extends DataModel> void create(UUID jobId, String key, T model) {
    try {
      create(createRowKey(jobId, key), JOB_DATA_TABLE, null, model);
    } catch (IOException e) {
      throw new MicrosoftStorageException("Error creating job: " + jobId, e);
    }
  }

  @Override
  public <T extends DataModel> void update(UUID jobId, String key, T model) {
    // TODO: implement update functionality
    throw new UnsupportedOperationException("Implement update functionality. ");
  }

  @Override
  public <T extends DataModel> T findData(UUID jobId, String key, Class<T> type) {
    return find(type, createRowKey(jobId, key), JOB_DATA_TABLE);
  }

  @Override
  public void removeData(UUID jobId, String key) {
    try {
      remove(jobId, JOB_DATA_TABLE);
    } catch (IOException e) {
      throw new MicrosoftStorageException("Unable to remove data for job: " + jobId);
    }
  }

  @Override
  public void create(UUID jobId, String key, InputStream stream) {
    try {
      CloudBlobContainer reference = blobClient.getContainerReference(BLOB_CONTAINER);
      CloudBlockBlob blob = reference.getBlockBlobReference(createRowKey(jobId, key));
      blob.upload(stream, UNKNOWN_LENGTH);
    } catch (StorageException | URISyntaxException | IOException e) {
      throw new MicrosoftStorageException("Error creating stream for job: " + jobId, e);
    }
  }

  @Override
  public InputStreamWrapper getStream(UUID jobId, String key) {
    try {
      CloudBlobContainer reference = blobClient.getContainerReference(BLOB_CONTAINER);
      CloudBlockBlob blob = reference.getBlockBlobReference(createRowKey(jobId, key));
      return new InputStreamWrapper(blob.openInputStream(), blob.getProperties().getLength());
    } catch (StorageException | URISyntaxException e) {
      throw new MicrosoftStorageException("Error returning stream for job: " + jobId, e);
    }
  }

  @Override
  public UUID findFirst(JobAuthorization.State jobState) {
    try {
      String partitionFilter =
          generateFilterCondition(
              "PartitionKey", TableQuery.QueryComparisons.EQUAL, configuration.getPartitionKey());
      String stateFilter =
          generateFilterCondition(
              "State",
              TableQuery.QueryComparisons.EQUAL,
              jobState.name()); // properties are converted to capitalized by the storage API

      String combinedFilter =
          TableQuery.combineFilters(partitionFilter, TableQuery.Operators.AND, stateFilter);

      TableQuery<DataWrapper> query =
          TableQuery.from(DataWrapper.class).where(combinedFilter).take(1);

      CloudTable table = tableClient.getTableReference(JOB_TABLE);
      Iterator<DataWrapper> iter = table.execute(query).iterator();
      if (!iter.hasNext()) {
        return null;
      }
      return UUID.fromString(iter.next().getRowKey());
    } catch (StorageException | URISyntaxException e) {
      throw new MicrosoftStorageException("Error finding first job", e);
    }
  }

  private void create(String rowKey, String tableName, String state, Object type) throws IOException {
    try {

      CloudTable table = tableClient.getTableReference(tableName);

      String serializedJob = configuration.getMapper().writeValueAsString(type);
      DataWrapper wrapper =
          new DataWrapper(
              configuration.getPartitionKey(),
              rowKey,
              state,
              serializedJob); // job id used as key
      TableOperation insert = TableOperation.insert(wrapper);
      table.execute(insert);
    } catch (JsonProcessingException | StorageException | URISyntaxException e) {
      throw new IOException("Error creating data for rowKey: " + rowKey, e);
    }
  }

  private void remove(UUID jobId, String tableName) throws IOException {
    try {

      CloudTable table = tableClient.getTableReference(tableName);
      TableOperation retrieve =
          TableOperation.retrieve(
              configuration.getPartitionKey(), jobId.toString(), DataWrapper.class);
      TableResult result = table.execute(retrieve);
      DataWrapper wrapper = result.getResultAsType();

      TableOperation delete = TableOperation.delete(wrapper);
      table.execute(delete);

    } catch (StorageException | URISyntaxException e) {
      throw new IOException("Error removing data for job: " + jobId, e);
    }
  }

  private <T> T find(Class<T> type, String rowKey, String tableName) {
    try {

      CloudTable table = tableClient.getTableReference(tableName);
      TableOperation retrieve =
          TableOperation.retrieve(
              configuration.getPartitionKey(), rowKey, DataWrapper.class);
      TableResult result = table.execute(retrieve);
      DataWrapper wrapper = result.getResultAsType();
      return configuration.getMapper().readValue(wrapper.getSerialized(), type);
    } catch (StorageException | IOException | URISyntaxException e) {
      throw new MicrosoftStorageException("Error finding data for rowKey: " + rowKey, e);
    }
  }

  private static String createRowKey(UUID jobId, String key) {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(key));
    return String.format("%s-%s", jobId.toString(), key);
  }
}
