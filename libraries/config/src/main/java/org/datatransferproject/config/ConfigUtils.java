/*
 * Copyright 2018 The Data Transfer Project Authors.
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

package org.datatransferproject.config;

import com.google.common.collect.ImmutableList;
import java.io.InputStream;
import java.io.SequenceInputStream;

/**
 * Common utilities for parsing configuration from files on the classpath.
 */
public class ConfigUtils {
  /**
   * Concatenates {@link InputStream}s to multiple configuration files on the classpath into a
   * single {@link InputStream}.
   */
  public static InputStream getCombinedInputStream(ImmutableList<String> settingsFiles) {
    return settingsFiles.stream()
        .map(file -> ConfigUtils.class.getClassLoader().getResourceAsStream(file))
        .reduce(null, (in1, in2) -> combineStreams(in1, in2));
  }

  /**
   * Concatenates two {@link InputStream}s.
   */
  private static InputStream combineStreams(InputStream in1, InputStream in2) {
    if (in1 != null) {
      if (in2 != null) {
        return new SequenceInputStream(in1, in2);
      }
      return in1;
    }
    // in1 == null
    if (in2 != null) {
      return in2;
    }
    return null;
  }
}
