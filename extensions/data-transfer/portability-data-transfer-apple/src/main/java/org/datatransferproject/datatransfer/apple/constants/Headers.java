/*
 * Copyright 2023 The Data Transfer Project Authors.
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

package org.datatransferproject.datatransfer.apple.constants;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nonnull;
import org.jetbrains.annotations.Nullable;

/** Http Header names. */
public enum Headers {
  AUTHORIZATION("Authorization"),
  CORRELATION_ID("X-Apple-Request-UUID");

  private final String value;

  Headers(final String value) {
    this.value = value;
  }

  @Nonnull
  public String getValue() {
    return value;
  }

  private static final Map<String, Headers> map = new HashMap<>();

  static {
    for (final Headers headers : Headers.values()) {
      map.put(headers.getValue().toLowerCase(), headers);
    }
  }

  @Nullable
  public static Headers forNameIgnoreCase(final String name) {
    return map.get(name.toLowerCase());
  }
}
