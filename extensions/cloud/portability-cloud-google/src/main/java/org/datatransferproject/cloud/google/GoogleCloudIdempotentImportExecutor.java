package org.datatransferproject.cloud.google;

import static java.lang.String.format;

import com.google.cloud.datastore.Datastore;
import com.google.common.base.Joiner;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
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

  private final Datastore datastore;
  private final Monitor monitor;

  private Map<String, Serializable> knownValues;
  private Map<String, ErrorDetail> errors;
  private UUID jobId;

  public GoogleCloudIdempotentImportExecutor(Datastore datastore, Monitor monitor) {
    this.datastore = datastore;
    this.monitor = monitor;
  }

  @Override
  public <T extends Serializable> T executeAndSwallowIOExceptions(String idempotentId,
      String itemName, Callable<T> callable) throws Exception {
    try {
      return executeOrThrowException(idempotentId, itemName, callable);
    } catch (IOException e) {
      // Note all errors are logged in executeOrThrowException so no need to re-log them here.
      return null;
    }
  }

  @Override
  public <T extends Serializable> T executeOrThrowException(String idempotentId, String itemName,
      Callable<T> callable) throws Exception {
    String jobIdPrefix = "Job " + jobId + ": ";

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

  private <T extends Serializable> void addResult(String idempotentId, T result) {
    knownValues.put(idempotentId, result);
    errors.remove(idempotentId);
  }

  private void addError(String idempotentId, ErrorDetail errorDetail) {
    errors.put(idempotentId, errorDetail);
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
    this.jobId = jobId;
    this.knownValues = getKnownValuesForJob(jobId);
    this.errors = getErrorDetailsForJob(jobId);
  }

  private Map<String, Serializable> getKnownValuesForJob(UUID jobId) {
    return new HashMap<>();
  }

  private Map<String, ErrorDetail> getErrorDetailsForJob(UUID jobId) {
    return new HashMap<>();
  }
}
