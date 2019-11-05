package org.datatransferproject.cloud.google;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.cloud.datastore.*;
import com.google.cloud.datastore.StructuredQuery.CompositeFilter;
import com.google.cloud.datastore.StructuredQuery.PropertyFilter;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.spi.transfer.idempotentexecutor.IdempotentImportExecutor;
import org.datatransferproject.types.transfer.errors.ErrorDetail;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;

import static java.lang.String.format;

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
        transaction.delete(getErrorKey(idempotentId, jobId));
        errors.remove(idempotentId);
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

  // In non-tests setJobId is only ever called once per executor, so the initialization of
  // knownValues and errors only happens once
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
          result.getString(IDEMPOTENT_ID_FIELD), result.getString(RESULTS_FIELD));
    }

    return dataStoreKnownValues;
  }

  private Map<String, ErrorDetail> getErrorDetailsForJob(UUID jobId) {
    Map<String, ErrorDetail> datastoreKnownErrors = new HashMap<>();
    Query<Entity> query =
        Query.newEntityQueryBuilder()
            .setKind(IDEMPONTENT_ERRORS_KIND)
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
    return createResultEntity(idempotentId, this.jobId, result);
  }

  @VisibleForTesting
  <T extends Serializable> Entity createResultEntity(String idempotentId, UUID jobId, T result)
      throws IOException {
    return GoogleCloudUtils.createEntityBuilder(
        getResultsKey(idempotentId, jobId),
        ImmutableMap.of(
            RESULTS_FIELD, result, JOB_ID_FIELD, jobId.toString(), IDEMPOTENT_ID_FIELD,
            idempotentId))
        .build();
  }

  private Key getResultsKey(String idempotentId, UUID jobId) {
    return datastore
        .newKeyFactory()
        .setKind(IDEMPOTENT_RESULTS_KIND)
        .newKey(jobId + "_" + idempotentId);
  }

  private Entity createErrorEntity(String idempotentId, ErrorDetail error) throws IOException {
    return createErrorEntity(idempotentId, this.jobId, error);
  }

  @VisibleForTesting
  Entity createErrorEntity(String idempotentId, UUID jobId, ErrorDetail error)
      throws IOException {
    return GoogleCloudUtils.createEntityBuilder(
        getErrorKey(idempotentId, jobId),
        ImmutableMap.of(
            ERROR_FIELD,
            objectMapper.writeValueAsString(error),
            JOB_ID_FIELD,
            jobId.toString(),
            IDEMPOTENT_ID_FIELD,
            idempotentId))
        .build();
  }

  private Key getErrorKey(String idempotentId, UUID jobId) {
    return datastore
        .newKeyFactory()
        .setKind(IDEMPONTENT_ERRORS_KIND)
        .newKey(jobId + "_" + idempotentId);
  }
}
