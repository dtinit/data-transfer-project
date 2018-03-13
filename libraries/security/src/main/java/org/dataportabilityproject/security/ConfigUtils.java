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

package org.dataportabilityproject.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.util.Map;

// TODO(rtannenbaum): Move this out of security/ into a new module config/. Couldn't figure it
// out in Intellij.
/**
 * Common utilities for yaml configuration parsing.
 */
public class ConfigUtils {

  /**
   * Parses an input stream to a yaml configuration file into a generic Map<String, Object>.
   */
  public static Map<String, Object> parse(InputStream in) throws IOException {
    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    return mapper.readValue(in, Map.class);
  }

  /**
   * Concatenates {@link InputStream}s to multiple yaml configuration files into a single
   * {@link InputStream}.
   */
  public static InputStream getSettingsCombinedInputStream(ImmutableList<String> settingsFiles) {
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
