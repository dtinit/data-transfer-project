/*
 * Copyright 2025 The Data Transfer Project Authors.
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
 *
 */

package org.datatransferproject.datatransfer.synology.utils;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class UrlUtilsTest {
  @Nested
  public class Join {
    @Test
    public void testWithTrailingSlash() {
      assertEquals("http://example.com/api", UrlUtils.join("http://example.com/", "api"));
      assertEquals("http://example.com/api", UrlUtils.join("http://example.com/", "/api"));
      assertEquals("http://example.com/api/v1", UrlUtils.join("http://example.com/", "api/v1"));
    }

    @Test
    public void testWithoutTrailingSlash() {
      assertEquals("http://example.com/api", UrlUtils.join("http://example.com", "/api"));
      assertEquals("http://example.com/api/v1", UrlUtils.join("http://example.com", "api/v1"));
    }

    @Test
    public void testWithEmptyUrl() {
      assertThrows(
          IllegalArgumentException.class,
          () -> {
            UrlUtils.join("", "api");
          });
    }

    @Test
    public void testWithEmptyPath() {
      assertEquals("http://example.com/", UrlUtils.join("http://example.com", ""));
    }

    @Test
    public void testWithNullUrlOrPath() {
      assertThrows(
          NullPointerException.class,
          () -> {
            UrlUtils.join(null, "api");
          });
      assertThrows(
          NullPointerException.class,
          () -> {
            UrlUtils.join("http://example.com", null);
          });
    }
  }
}
