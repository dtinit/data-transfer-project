/*
 * Copyright 2022 The Data Transfer Project Authors.
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

package org.datatransferproject.types.common.models;

import static org.junit.Assert.*;

import org.junit.Test;

public class MediaObjectTest {

  @Test
  public void testNullContentUrl() {
    MediaObject obj = new MediaObject("123");
    assertThrows(NullPointerException.class, () -> obj.setContentUrl((String) null));
  }

  @Test
  public void testIllegalContentUrl() {
    MediaObject obj = new MediaObject("123");

    assertNull(obj.getContentUrl());
    obj.setContentUrl("i am not an uri");
    assertNull(obj.getContentUrl());
  }

  @Test
  public void testEmptyContentUrl() {
    MediaObject obj = new MediaObject("123");

    assertNull(obj.getContentUrl());
    obj.setContentUrl("");
    assertNotNull(obj.getContentUrl());
  }
}