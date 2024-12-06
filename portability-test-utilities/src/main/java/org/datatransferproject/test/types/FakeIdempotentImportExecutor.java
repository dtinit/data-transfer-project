package org.datatransferproject.test.types;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.Callable;
import org.datatransferproject.spi.transfer.idempotentexecutor.IdempotentImportExecutor;
import org.datatransferproject.types.transfer.errors.ErrorDetail;

public class FakeIdempotentImportExecutor implements IdempotentImportExecutor {

  private HashMap<String, Serializable> knownValues = new HashMap<>();

  @Override
  public <T extends Serializable> T executeAndSwallowIOExceptions(
      String idempotentId, String itemName, Callable<T> callable) throws Exception {
    try {
      return executeOrThrowException(idempotentId, itemName, callable);
    } catch (IOException e) {
      return null;
    }
  }

  @Override
  public <T extends Serializable> T executeOrThrowException(
      String idempotentId, String itemName, Callable<T> callable) throws Exception {
    if (knownValues.containsKey(idempotentId)) {
      System.out.println("Using cached key " + idempotentId + " from cache");
      return (T) knownValues.get(idempotentId);
    }
    try {
      T result = callable.call();
      knownValues.put(idempotentId, result);
      System.out.println("Storing key " + idempotentId + " in cache");
      return result;
    } catch (Exception e) {
      throw e;
    }
  }

  @Override
  public <T extends Serializable> T getCachedValue(String idempotentId) {
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
    return ImmutableList.of();
  }

  @Override
  public void setJobId(UUID jobId) {
    // We deliberately do nothing here as this class is Fake and not behaviour which needs to be faked
  }
}
