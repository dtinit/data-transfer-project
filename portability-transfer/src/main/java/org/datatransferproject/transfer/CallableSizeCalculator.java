package org.datatransferproject.transfer;

import static org.datatransferproject.spi.api.transport.DiscardingStreamCounter.discardForLength;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Callable;
import org.datatransferproject.spi.cloud.connection.ConnectionProvider;
import org.datatransferproject.spi.cloud.storage.TemporaryPerJobDataStore.InputStreamWrapper;
import org.datatransferproject.types.common.DownloadableItem;

public class CallableSizeCalculator implements Callable<Map<String, Long>> {

  private final UUID jobId;
  private final ConnectionProvider connectionProvider;
  private final Collection<? extends DownloadableItem> items;

  public CallableSizeCalculator(
      UUID jobId,
      ConnectionProvider connectionProvider,
      Collection<? extends DownloadableItem> items) {
    this.jobId = Objects.requireNonNull(jobId);
    this.connectionProvider = Objects.requireNonNull(connectionProvider);
    this.items = Objects.requireNonNull(items);
  }

  @Override
  public Map<String, Long> call() throws Exception {
    Map<String, Long> result = new LinkedHashMap<>();
    for (DownloadableItem item : items) {
      InputStreamWrapper stream = connectionProvider.getInputStreamForItem(jobId, item);
      long size = stream.getBytes();
      if (size <= 0) {
        size = discardForLength(stream.getStream());
      }

      result.put(item.getIdempotentId(), size);
    }

    return result;
  }
}
