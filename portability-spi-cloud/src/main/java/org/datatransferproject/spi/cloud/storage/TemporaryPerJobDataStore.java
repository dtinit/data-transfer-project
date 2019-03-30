package org.datatransferproject.spi.cloud.storage;

import org.datatransferproject.types.common.models.DataModel;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

/**
 * A store for data that will be persisted for the life of a backup job.
 *
 * <p>This class is intended to be implemented by extensions that support storage in various
 * back-end services.
 */
public interface TemporaryPerJobDataStore {
  default <T extends DataModel> void create(UUID jobId, String key, T model) throws IOException {
    throw new UnsupportedOperationException();
  }

  /**
   * Updates the given model instance associated with a job.
   */
  default <T extends DataModel> void update(UUID jobId, String key, T model) {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns a model instance for the id of the given type or null if not found.
   */
  default <T extends DataModel> T findData(UUID jobId, String key, Class<T> type)
      throws IOException {
    throw new UnsupportedOperationException();
  }

  /**
   * Removes the data model instance.
   */
  default void removeData(UUID JobId, String key) {
    throw new UnsupportedOperationException();
  }

  default void create(UUID jobId, String key, InputStream stream) throws IOException {
    throw new UnsupportedOperationException();
  }

  default InputStream getStream(UUID jobId, String key) throws IOException {
    throw new UnsupportedOperationException();
  }
}
