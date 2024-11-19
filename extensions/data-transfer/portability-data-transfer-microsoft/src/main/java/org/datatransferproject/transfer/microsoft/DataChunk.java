package org.datatransferproject.transfer.microsoft;

import com.google.auto.value.AutoValue;

/** Describe small buffers of bytes captured from a large java.io Stream. */
@AutoValue
public abstract class DataChunk {
  /** Bytes being held in this buffer. */
  public abstract byte[] chunk();

  /** Byte count of {@link chunk}. */
  public int size() {
    return chunk().length;
  }

  /** Index-offset within the original java.io Stream at which {@link chunk} had started. */
  public abstract long streamByteOffset();

  /**
   * Index-offset within the original java.io Stream at which the final byte of {@link chunk} lived.
   */
  public long finalByteOffset() {
    return streamByteOffset() + size() - 1;
  }

  public static Builder builder() {
    return new org.datatransferproject.transfer.microsoft.AutoValue_DataChunk.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setChunk(byte[] value);

    public abstract Builder setStreamByteOffset(long value);

    public abstract DataChunk build();
  }
}
