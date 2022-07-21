package org.datatransferproject.types.common;

/**
 * Represent an item we expect to be recognizable as an ordinary file on someone's computer.
 */
public interface Fileable {
  /**
   * Returns a standard MIME-type string one would expect to find if they inspected the downloaded
   * version of this file with standard tools on their PC.
   *
   * See https://en.wikipedia.org/wiki/Media_type
   *
   * NOTE: this should match {@link MediaObject#getEncodingFormat} behavior.
   */
  String getMimeType();
}
