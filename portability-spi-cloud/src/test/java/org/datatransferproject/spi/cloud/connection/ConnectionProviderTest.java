package org.datatransferproject.spi.cloud.connection;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.truth.Truth;
import java.util.UUID;
import org.datatransferproject.spi.cloud.storage.TemporaryPerJobDataStore;
import org.datatransferproject.spi.cloud.storage.TemporaryPerJobDataStore.InputStreamWrapper;
import org.datatransferproject.types.common.DownloadableItem;
import org.datatransferproject.types.common.models.photos.PhotoModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ConnectionProviderTest {

  private TemporaryPerJobDataStore jobStore;
  private ConnectionProvider connectionProvider;

  @BeforeEach
  public void setUp() throws Exception {
    jobStore = mock(TemporaryPerJobDataStore.class);
    connectionProvider = new ConnectionProvider(jobStore);
  }

  @Test
  public void getInputStreamFromTempStore() throws Exception {
    long expectedBytes = 323;
    when(jobStore.getStream(any(), anyString())).thenReturn(
        new InputStreamWrapper(null, expectedBytes));
    boolean inTempStore = true;
    String fetchableUrl = "https://example.com";
    DownloadableItem item = new PhotoModel("title", fetchableUrl, "description", "jpeg",
        "123", "album", inTempStore);
    UUID jobId = UUID.randomUUID();
    InputStreamWrapper streamWrapper = connectionProvider.getInputStreamForItem(
        jobId, item);

    Truth.assertThat(streamWrapper.getBytes()).isEqualTo(expectedBytes);
    verify(jobStore).getStream(eq(jobId), eq(fetchableUrl));
  }
}
