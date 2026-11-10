package org.datatransferproject.spi.cloud.connection;

import java.io.IOException;
import java.util.UUID;
import org.datatransferproject.spi.cloud.storage.TemporaryPerJobDataStore.InputStreamWrapper;
import org.datatransferproject.types.common.DownloadableItem;

/**
 * A {@link DownloadableItem} that downloads itself.
 *
 * <p>Implement this when the download needs an auth header. {@link ConnectionProvider} will call
 * {@link #openStream} instead of opening the item's URL or reading staged bytes, so the credential
 * stays in your extension and nothing is staged.
 */
public interface SelfFetchingItem extends DownloadableItem {

  /**
   * Opens a stream over this item's content.
   *
   * @param jobId the job the fetch belongs to, for credential lookup and logging
   */
  InputStreamWrapper openStream(UUID jobId) throws IOException;
}
