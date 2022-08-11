package org.datatransferproject.transfer;

import static org.apache.commons.lang3.RandomUtils.nextInt;
import static org.apache.commons.lang3.RandomUtils.nextLong;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.truth.Truth;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.datatransferproject.spi.cloud.connection.ConnectionProvider;
import org.datatransferproject.spi.cloud.storage.TemporaryPerJobDataStore.InputStreamWrapper;
import org.datatransferproject.types.common.DownloadableItem;
import org.datatransferproject.types.common.models.photos.PhotoModel;
import org.junit.Before;
import org.junit.Test;


public class CallableSizeCalculatorTest {

  private UUID jobId;
  private ConnectionProvider connectionProvider;

  @Before
  public void setUp() throws Exception {
    jobId = UUID.randomUUID();
    connectionProvider = mock(ConnectionProvider.class);
  }

  @Test
  public void testAllSizesAreProvided() throws Exception {
    List<DownloadableItem> items = IntStream.range(1, 20)
        .mapToObj(i -> createItem(i + "-" + nextInt(100, 9999)))
        .collect(Collectors.toList());

    Map<String, Long> expected = new HashMap<>();
    for (DownloadableItem item : items) {
      long size = nextLong(1, 100000);
      when(connectionProvider.getInputStreamForItem(eq(jobId), eq(item)))
          .thenReturn(new InputStreamWrapper(null, size));
      expected.put(item.getIdempotentId(), size);
    }

    Map<String, Long> actual = new CallableSizeCalculator(jobId, connectionProvider,
        items).call();

    Truth.assertThat(actual).containsExactlyEntriesIn(expected);
  }

  @Test
  public void testSizeIsNotProvided() throws Exception {
    DownloadableItem item = createItem("1-" + nextInt(100, 9999));

    int size = nextInt(1, 1024 * 1024 * 42); // 42MB max
    byte[] bytes = new byte[size];
    Arrays.fill(bytes, (byte) 0);
    InputStream inputStream = new ByteArrayInputStream(bytes);
    when(connectionProvider.getInputStreamForItem(eq(jobId), eq(item)))
        .thenReturn(new InputStreamWrapper(inputStream, -1L));

    Map<String, Long> expected = Collections.singletonMap(item.getIdempotentId(), (long) size);

    Map<String, Long> actual = new CallableSizeCalculator(jobId, connectionProvider,
        Collections.singleton(item)).call();

    Truth.assertThat(actual).containsExactlyEntriesIn(expected);

    // Make sure the input stream was read in full
    int nextByte = inputStream.read();
    Truth.assertThat(nextByte).isEqualTo(-1);
  }

  @Test
  public void testExceptionIsThrown() throws Exception {
    when(connectionProvider.getInputStreamForItem(any(), any()))
        .thenThrow(new IOException("oh no!"));
    assertThrows(IOException.class, () -> {
      new CallableSizeCalculator(jobId, connectionProvider,
          Collections.singleton(createItem("1-3242"))).call();
    });
  }

  private DownloadableItem createItem(String dataId) {
    return new PhotoModel("title", "url", "description", "jpeg",
        dataId, "album", false);
  }
}
