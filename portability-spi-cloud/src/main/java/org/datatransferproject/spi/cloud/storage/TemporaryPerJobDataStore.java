package org.datatransferproject.spi.cloud.storage;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import org.datatransferproject.types.common.models.DataModel;

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

  /** Updates the given model instance associated with a job. */
  default <T extends DataModel> void update(UUID jobId, String key, T model) {
    throw new UnsupportedOperationException();
  }

  /** Returns a model instance for the id of the given type or null if not found. */
  default <T extends DataModel> T findData(UUID jobId, String key, Class<T> type)
      throws IOException {
    throw new UnsupportedOperationException();
  }

  /** Removes the data model instance. */
  default void removeData(UUID JobId, String key) {
    throw new UnsupportedOperationException();
  }

  default void create(UUID jobId, String key, InputStream stream) throws IOException {
    throw new UnsupportedOperationException();
  }

  default InputStreamWrapper getStream(UUID jobId, String key) throws IOException {
    throw new UnsupportedOperationException();
  }

  default File getTempFileFromInputStream(InputStream inputStream, String prefix, String suffix)
      throws IOException {
    File tmp = Files.createTempFile(prefix, suffix).toFile();
    tmp.deleteOnExit();
    Files.copy(inputStream, tmp.toPath(), StandardCopyOption.REPLACE_EXISTING);
    return tmp;
  }

  public class InputStreamWrapper {

    private final InputStream stream;
    private final Long bytes;

    public InputStreamWrapper(InputStream stream) {
      this.stream = stream;
      bytes = 0L;
    }

    public InputStreamWrapper(InputStream stream, Long bytes) {
      this.stream = stream;
      this.bytes = bytes;
    }

    public InputStream getStream() {
      return stream;
    }

    public Long getBytes() {
      return bytes;
    }
  }
}
