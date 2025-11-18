package org.datatransferproject.transfer.microsoft;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

/**
 * Allows tracking reads across a stream.
 *
 * <p>Does not close the held input stream.
 */
public class StreamChunker {
  private final int chunkSizeBytes;
  private final InputStream inputStream;

  private long streamByteOffset = 0;

  public StreamChunker(int chunkSizeBytes, InputStream inputStream) {
    this.inputStream = inputStream;
    this.chunkSizeBytes = chunkSizeBytes;
  }

  /**
   * Constructs a new DataChunk from just {@code chunkSizeBytes} bytes of the stream.
   *
   * <p>Returned chunk will be less than or equal to chunkSizeBytes, or absent if no bytes were
   * remaining in the stream.
   */
  public Optional<DataChunk> nextChunk() throws IOException {
    byte[] chunkOfData = inputStream.readNBytes(chunkSizeBytes);
    Optional<DataChunk> resp =
        chunkOfData.length == 0
            ? Optional.empty()
            : Optional.of(
                DataChunk.builder()
                    .setChunk(chunkOfData)
                    .setStreamByteOffset(streamByteOffset)
                    .build());
    streamByteOffset += chunkOfData.length;
    return resp;
  }
}
