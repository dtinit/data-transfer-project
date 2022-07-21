package org.datatransferproject.spi.cloud.connection;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;
import org.datatransferproject.spi.cloud.storage.TemporaryPerJobDataStore;
import org.datatransferproject.spi.cloud.storage.TemporaryPerJobDataStore.InputStreamWrapper;
import org.datatransferproject.types.common.DownloadableItem;

public class ConnectionProvider {

  private final TemporaryPerJobDataStore jobStore;

  public ConnectionProvider(TemporaryPerJobDataStore jobStore) {
    this.jobStore = jobStore;
  }

  public InputStreamWrapper getInputStreamForItem(UUID jobId, DownloadableItem item)
      throws IOException {

    String fetchableUrl = item.getFetchableUrl();
    if (item.isInTempStore()) {
      return jobStore.getStream(jobId, fetchableUrl);
    }

    HttpURLConnection conn = getConnection(fetchableUrl);
    return new InputStreamWrapper(
        conn.getInputStream(), Math.max(conn.getContentLengthLong(), 0));
  }

  public static HttpURLConnection getConnection(String urlStr) throws IOException {
    URL url = new URL(urlStr);
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    conn.connect();
    return conn;
  }
}
