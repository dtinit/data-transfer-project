/*
 * Copyright 2021 The Data Transfer Project Authors.
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

package org.datatransferproject.datatransfer.google.i18n;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class GoogleMultilingualDictionaryTest {
  @Test
  public void getITStrings() {
    GoogleMultilingualDictionary sut = new GoogleMultilingualDictionary("it-IT");
    String actualCopyOf = sut.get(GoogleMultilingualString.CopyOf);
    assertEquals("Copia di {0}", actualCopyOf);
  }

  @Test
  public void getRUStrings() {
    GoogleMultilingualDictionary sut = new GoogleMultilingualDictionary("ru");
    String actualCopyOf = sut.get(GoogleMultilingualString.CopyOf);
    assertEquals("Копия {0}", actualCopyOf);
  }

  @Test
  public void getARStrings() {
    GoogleMultilingualDictionary sut = new GoogleMultilingualDictionary("ar");
    String actualCopyOf = sut.get(GoogleMultilingualString.CopyOf);
    assertEquals("نسخة من {0}", actualCopyOf);
  }

  @Test
  public void getZHStrings() {
    GoogleMultilingualDictionary sut = new GoogleMultilingualDictionary("zh");
    String actualCopyOf = sut.get(GoogleMultilingualString.CopyOf);
    assertEquals("{0}副本", actualCopyOf);
  }

  @Test
  public void getNonExistentLocale() {
    GoogleMultilingualDictionary sut = new GoogleMultilingualDictionary("12");
    String actualCopyOf = sut.get(GoogleMultilingualString.CopyOf);
    assertEquals("Copy of {0}", actualCopyOf);
  }

  @Test
  public void getNullLocale() {
    GoogleMultilingualDictionary sut = new GoogleMultilingualDictionary(null);
    String actualCopyOf = sut.get(GoogleMultilingualString.CopyOf);
    assertEquals("Copy of {0}", actualCopyOf);
  }

  @Test
  public void getEmptyLocale() {
    GoogleMultilingualDictionary sut = new GoogleMultilingualDictionary("");
    String actualCopyOF = sut.get(GoogleMultilingualString.CopyOf);
    assertEquals("Copy of {0}", actualCopyOF);
  }
}
