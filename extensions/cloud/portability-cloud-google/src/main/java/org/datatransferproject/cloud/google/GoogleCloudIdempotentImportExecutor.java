package org.datatransferproject.cloud.google;

import static java.lang.String.format;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreException;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.Query;
import com.google.cloud.datastore.QueryResults;
import com.google.cloud.datastore.StructuredQuery.CompositeFilter;
import com.google.cloud.datastore.StructuredQuery.PropertyFilter;
import com.google.cloud.datastore.Transaction;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.spi.transfer.idempotentexecutor.IdempotentImportExecutor;
import org.datatransferproject.types.transfer.errors.ErrorDetail;

public class GoogleCloudIdempotentImportExecutor implements IdempotentImportExecutor {

  private static final String IDEMPOTENT_RESULTS_KIND = "IdempotentResults";
  private static final String IDEMPONTENT_ERRORS_KIND = "IdempotentErrors";

  private static final String RESULTS_FIELD = "result_details";
  private static final String JOB_ID_FIELD = "job_id";
  private static final String IDEMPOTENT_ID_FIELD = "idempotent_id";
  private static final String ERROR_FIELD = "error_details";

  private final Datastore datastore;
  private final Monitor monitor;
  private final ObjectMapper objectMapper;

  // These are all variables corresponding to the job state. Only initialized when setJobId() is called
  private Map<String, Serializable> knownValues;
  private Map<String, ErrorDetail> errors;
  private UUID jobId;
  private String jobIdPrefix;

  public GoogleCloudIdempotentImportExecutor(Datastore datastore, Monitor monitor) {
    this.datastore = datastore;
    this.monitor = monitor;
    this.objectMapper = new ObjectMapper();
    this.objectMapper.registerModule(new JavaTimeModule());
    this.objectMapper.registerSubtypes(ErrorDetail.class);
  }

  @Override
  public <T extends Serializable> T executeAndSwallowIOExceptions(
      String idempotentId, String itemName, Callable<T> callable) throws Exception {
    try {
      return executeOrThrowException(idempotentId, itemName, callable);
    } catch (IOException e) {
      // Note all errors are logged in executeOrThrowException so no need to re-log them here.
      return null;
    }
  }

  @Override
  public <T extends Serializable> T executeOrThrowException(
      String idempotentId, String itemName, Callable<T> callable) throws Exception {
    Preconditions.checkNotNull(jobId, "executing a callable before initialization of a job");

    if (knownValues.containsKey(idempotentId)) {
      monitor.debug(
          () ->
              jobIdPrefix
                  + format("Using cached key %s from cache for %s", idempotentId, itemName));
      return (T) knownValues.get(idempotentId);
    }

    try {
      T result = callable.call();
      monitor.debug(
          () -> jobIdPrefix + format("Storing key %s in cache for %s", idempotentId, itemName));
      addResult(idempotentId, result);
      return result;
    } catch (Exception e) {
      ErrorDetail errorDetail =
          ErrorDetail.builder()
              .setId(idempotentId)
              .setTitle(itemName)
              .setException(Throwables.getStackTraceAsString(e))
              .build();
      addError(idempotentId, errorDetail);
      monitor.severe(() -> jobIdPrefix + "Problem with importing item: " + errorDetail);
      throw e;
    }
  }

  private <T extends Serializable> void addResult(String idempotentId, T result)
      throws IOException {
    knownValues.put(idempotentId, result);

    try {
      Transaction transaction = datastore.newTransaction();

      transaction.put(createResultEntity(idempotentId, result));
      if (errors.containsKey(idempotentId)) {
        // if the errors contain this key, that means the ID
        transaction.delete(getErrorKey(idempotentId));
      }
      transaction.commit();
    } catch (DatastoreException e) {
      monitor.severe(() -> jobIdPrefix + "Error writing result to datastore: " + e);
    }
  }

  private void addError(String idempotentId, ErrorDetail errorDetail) throws IOException {
    errors.put(idempotentId, errorDetail);
    try {
      Transaction transaction = datastore.newTransaction();
      transaction.put(createErrorEntity(idempotentId, errorDetail));
      transaction.commit();
    } catch (DatastoreException e) {
      monitor.severe(() -> jobIdPrefix + "Error writing ErrorDetails to datastore: " + e);
    }
  }

  @Override
  public <T extends Serializable> T getCachedValue(String idempotentId)
      throws IllegalArgumentException {
    if (!knownValues.containsKey(idempotentId)) {
      throw new IllegalArgumentException(
          idempotentId
              + " is not a known key, known keys: "
              + Joiner.on(", ").join(knownValues.keySet()));
    }
    return (T) knownValues.get(idempotentId);
  }

  @Override
  public boolean isKeyCached(String idempotentId) {
    return knownValues.containsKey(idempotentId);
  }

  @Override
  public Collection<ErrorDetail> getErrors() {
    return ImmutableList.copyOf(errors.values());
  }

  @Override
  public void setJobId(UUID jobId) {
    Preconditions.checkNotNull(jobId);
    this.jobId = jobId;
    this.knownValues = getKnownValuesForJob(jobId);
    this.errors = getErrorDetailsForJob(jobId);
    jobIdPrefix = "Job " + jobId + ": ";
  }

  private Map<String, Serializable> getKnownValuesForJob(UUID jobId) {
    Map<String, Serializable> dataStoreKnownValues = new HashMap<>();
    Query<Entity> query =
        Query.newEntityQueryBuilder()
            .setKind(IDEMPOTENT_RESULTS_KIND)
            .setFilter(CompositeFilter.and(PropertyFilter.eq(JOB_ID_FIELD, String.valueOf(jobId))))
            .build();
    QueryResults<Entity> results = datastore.run(query);

    while (results.hasNext()) {
      Entity result = results.next();
      dataStoreKnownValues.put(
          result.getString(IDEMPOTENT_ID_FIELD), result.getBlob(RESULTS_FIELD));
    }

    return dataStoreKnownValues;
  }

  private Map<String, ErrorDetail> getErrorDetailsForJob(UUID jobId) {
    Map<String, ErrorDetail> datastoreKnownErrors = new HashMap<>();
    Query<Entity> query =
        Query.newEntityQueryBuilder()
            .setKind(IDEMPOTENT_RESULTS_KIND)
            .setFilter(CompositeFilter.and(PropertyFilter.eq(JOB_ID_FIELD, String.valueOf(jobId))))
            .build();
    QueryResults<Entity> results = datastore.run(query);

    while (results.hasNext()) {
      Entity result = results.next();
      try {
        ErrorDetail error =
            objectMapper.readerFor(ErrorDetail.class).readValue(result.getString(ERROR_FIELD));
        datastoreKnownErrors.put(result.getString(IDEMPOTENT_ID_FIELD), error);
      } catch (IOException e) {
        monitor.severe(() -> jobIdPrefix + "Unable to parse ErrorDetail: " + e);
        throw new IllegalStateException(e);
      }
    }

    return datastoreKnownErrors;
  }

  private <T extends Serializable> Entity createResultEntity(String idempotentId, T result)
      throws IOException {
    return GoogleCloudUtils.createEntityBuilder(
            getResultsKey(idempotentId),
            ImmutableMap.of(
                RESULTS_FIELD, result, JOB_ID_FIELD, jobId, IDEMPOTENT_ID_FIELD, idempotentId))
        .build();
  }

  private Key getResultsKey(String idempotentId) {
    return datastore
        .newKeyFactory()
        .setKind(IDEMPOTENT_RESULTS_KIND)
        .newKey(jobId + "_" + idempotentId);
  }

  private Entity createErrorEntity(String idempotentId, ErrorDetail error) throws IOException {
    return GoogleCloudUtils.createEntityBuilder(
            getResultsKey(idempotentId),
            ImmutableMap.of(
                ERROR_FIELD,
                objectMapper.writeValueAsString(error),
                JOB_ID_FIELD,
                jobId,
                IDEMPOTENT_ID_FIELD,
                idempotentId))
        .build();
  }

  private Key getErrorKey(String idempotentId) {
    return datastore
        .newKeyFactory()
        .setKind(IDEMPONTENT_ERRORS_KIND)
        .newKey(jobId + "_" + idempotentId);
  }
}
