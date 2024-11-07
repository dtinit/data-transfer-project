package org.datatransferproject.transfer.microsoft;

import com.google.auto.value.AutoValue;

/**
 * This utility class allows us to break up an InputStream into multiple chunks for part-by-part
 * upload to a service, for example to be consumed in an upload session.
 */
@AutoValue
public abstract class DataChunk {
  public abstract byte[] chunk();

  public abstract int streamByteOffset();

  public static Builder builder() {
    return new org.datatransferproject.transfer.microsoft.AutoValue_DataChunk.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setChunk(byte[] value);

    public abstract Builder setStreamByteOffset(int value);

    public abstract DataChunk build();
  }

  public int size() {
    return chunk().length;
  }

  public int finalByteOffset() {
    return streamByteOffset() + size() - 1;
  }
}
