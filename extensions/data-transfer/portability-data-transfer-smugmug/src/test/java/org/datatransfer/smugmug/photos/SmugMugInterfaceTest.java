/*
 * Copyright 2019 The Data Transfer Project Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.datatransferproject.transfer.smugmug.photos;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SmugMugInterfaceTest {

  @Test
  public void cleanName_standard() {
    assertEquals(SmugMugInterface.cleanName("MyAlbum"), "MyAlbum");
  }

  @Test
  public void cleanName_punctuation() {
    assertEquals(SmugMugInterface.cleanName("MyAlbum!"), "MyAlbum");
  }

  @Test
  public void cleanName_spaces() {
    assertEquals(SmugMugInterface.cleanName("My Album"), "My-Album");
  }

  @Test
  public void cleanName_long() {
    assertEquals(
        SmugMugInterface.cleanName(
            "My Album From That One Time I did an Activity and took several" + " pictures of it"),
        "My-Album-From-That-One-Time-I-did-an-Act");
  }

  @Test
  public void cleanName_NonLatin() {
    assertEquals(SmugMugInterface.cleanName("ჩემი ფოტოები"), "ჩემი-ფოტოები");
  }

  @Test
  public void cleanName_AllWrong() {
    assertEquals(SmugMugInterface.cleanName("🔥"), "");
  }
}
