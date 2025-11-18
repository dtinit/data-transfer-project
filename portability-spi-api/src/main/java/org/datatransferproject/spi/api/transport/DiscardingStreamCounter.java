package org.datatransferproject.spi.api.transport;

import com.google.common.io.CountingInputStream;
import java.io.IOException;
import java.io.InputStream;

public class DiscardingStreamCounter {
  private DiscardingStreamCounter() {}

  /** Returns byte size of stream, discarding its contents and closing the stream. */
  public static long discardForLength(InputStream stream) throws IOException {
    CountingInputStream counter = new CountingInputStream(stream);
    while (true) {
      if (counter.skip(Integer.MAX_VALUE) < Integer.MAX_VALUE) {
        counter.close();
        return counter.getCount();
      }
    }
  }
}
