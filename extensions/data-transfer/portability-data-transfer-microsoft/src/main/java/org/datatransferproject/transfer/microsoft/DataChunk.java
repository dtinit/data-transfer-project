package org.datatransferproject.transfer.microsoft;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
  This utility class allows us to break up an InputStream into multiple chunks
  for part-by-part upload to a service, for example to be consumed in an upload session.
*/
public class DataChunk {
  private static final int CHUNK_SIZE = 32000 * 1024; // 32000KiB

  private final byte[] data;
  private final int size;
  private final int rangeStart;
  public DataChunk(byte[] data, int size, int rangeStart) {
    this.data = data;
    this.size = size;
    this.rangeStart = rangeStart;
  }

  public int getSize() {
    return size;
  }

  public byte[] getData() {
    return data;
  }

  public int getStart() {
    return rangeStart;
  }

  public int getEnd() {
    return rangeStart + size - 1;
  }

  public static List<DataChunk> splitData(InputStream inputStream) throws IOException {
    ArrayList<DataChunk> chunksToSend = new ArrayList();
    byte[] data = new byte[CHUNK_SIZE];
    int totalFileSize = 0;
    int quantityToSend;
    int roomLeft = CHUNK_SIZE;
    int offset = 0;
    int chunksRead = 0;

    // start timing
    while ((quantityToSend = inputStream.read(data, offset, roomLeft)) != -1) {
      offset += quantityToSend;
      roomLeft -= quantityToSend;
      if (roomLeft == 0) {
        chunksToSend.add(new DataChunk(data, CHUNK_SIZE, chunksRead * CHUNK_SIZE));
        chunksRead++;
        roomLeft = CHUNK_SIZE;
        offset = 0;
        totalFileSize += CHUNK_SIZE;
        data = new byte[CHUNK_SIZE];
      }
    }
    if (offset != 0) {
      chunksToSend.add(new DataChunk(data, offset, chunksRead * CHUNK_SIZE));
      totalFileSize += offset;
      chunksRead++;
    }
    return chunksToSend;
  }

}