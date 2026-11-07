package org.datatransferproject.spi.cloud.connection;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.truth.Truth;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.UUID;
import javax.annotation.Nonnull;
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

  @Test
  public void selfFetchingItemResolvesItselfAndNeverTouchesTheStore() throws Exception {
    long expectedBytes = 17;
    SelfFetchingTestItem item = new SelfFetchingTestItem(expectedBytes, /* inTempStore= */ false);
    UUID jobId = UUID.randomUUID();

    InputStreamWrapper streamWrapper = connectionProvider.getInputStreamForItem(jobId, item);

    Truth.assertThat(streamWrapper.getBytes()).isEqualTo(expectedBytes);
    Truth.assertThat(item.seenJobId).isEqualTo(jobId);
    verify(jobStore, never()).getStream(any(), anyString());
  }

  @Test
  public void selfFetchingItemTakesPrecedenceOverTheTempStore() throws Exception {
    // The branch order matters: an item that can fetch itself must do so even if it also claims
    // to be staged, otherwise the store would be consulted for bytes that were never written.
    SelfFetchingTestItem item = new SelfFetchingTestItem(5, /* inTempStore= */ true);

    connectionProvider.getInputStreamForItem(UUID.randomUUID(), item);

    verify(jobStore, never()).getStream(any(), anyString());
  }

  /** Minimal item that fetches its own bytes, standing in for a provider-side implementation. */
  private static final class SelfFetchingTestItem implements SelfFetchingItem {

    private final long bytes;
    private final boolean inTempStore;
    private UUID seenJobId;

    SelfFetchingTestItem(long bytes, boolean inTempStore) {
      this.bytes = bytes;
      this.inTempStore = inTempStore;
    }

    @Override
    public InputStreamWrapper openStream(UUID jobId) throws IOException {
      this.seenJobId = jobId;
      return new InputStreamWrapper(new ByteArrayInputStream(new byte[(int) bytes]), bytes);
    }

    @Override
    public String getFetchableUrl() {
      // Deliberately not a URL: a self-fetching item is never asked to produce a fetchable one.
      return "provider:opaque-id";
    }

    @Override
    public boolean isInTempStore() {
      return inTempStore;
    }

    @Nonnull
    @Override
    public String getIdempotentId() {
      return "provider:opaque-id";
    }
  }
}
