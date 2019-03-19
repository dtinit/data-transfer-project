package org.datatransferproject.test.types;

import com.google.common.base.Joiner;
import org.datatransferproject.spi.transfer.provider.IdempotentImportExecutor;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.concurrent.Callable;

public class FakeIdempotentImportExecutor implements IdempotentImportExecutor {
  private HashMap<String, Serializable> knownValues = new HashMap<>();
  @Override
  public <T extends Serializable> T execute(
      String idempotentId,
      String itemName,
      Callable<T> callable) throws IOException {
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
      throw new IOException("Problem executing callable for: " + idempotentId, e);
    }
  }

  @Override
  public <T extends Serializable> T getCachedValue(String idempotentId) {
    if (!knownValues.containsKey(idempotentId)) {
      throw new IllegalArgumentException(
          idempotentId + " is not a known key, known keys: "
              + Joiner.on(", ").join(knownValues.keySet()));
    }
    return (T) knownValues.get(idempotentId);
  }
}
