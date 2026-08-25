/*
 * Copyright 2026 The Data Transfer Project Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.datatransferproject.transfer.amazon.photos;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * MD5 utilities for Amazon Photos upload APIs.
 * MD5 is used here as an API-mandated content checksum, not for security.
 */
final class Md5Utils {

  private Md5Utils() {}

  /** Creates a new MD5 MessageDigest instance. */
  static MessageDigest newDigest() {
    try {
      return MessageDigest.getInstance("MD5");
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("MD5 algorithm not available", e);
    }
  }

  /** Converts a byte array to its lowercase hex string representation. */
  static String toHexString(byte[] bytes) {
    StringBuilder hex = new StringBuilder(bytes.length * 2);
    for (byte b : bytes) {
      hex.append(String.format("%02x", b));
    }
    return hex.toString();
  }

  /** Computes MD5 hex of a file region using a small buffer (no large heap allocation). */
  static String computeRegionMd5(RandomAccessFile raf, long offset, long length)
      throws IOException {
    MessageDigest md = newDigest();
    raf.seek(offset);
    byte[] buf = new byte[8192];
    long remaining = length;
    while (remaining > 0) {
      int toRead = (int) Math.min(buf.length, remaining);
      raf.readFully(buf, 0, toRead);
      md.update(buf, 0, toRead);
      remaining -= toRead;
    }
    return toHexString(md.digest());
  }
}
