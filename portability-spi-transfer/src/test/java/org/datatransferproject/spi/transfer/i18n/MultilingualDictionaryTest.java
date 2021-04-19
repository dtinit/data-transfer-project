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

package org.datatransferproject.spi.transfer.i18n;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class MultilingualDictionaryTest {
  @Test
  public void getITStrings() {
    BaseMultilingualDictionary sut = new BaseMultilingualDictionary("it-IT");
    String actualCopyOf = sut.get(BaseMultilingualString.CopyOf);
    assertEquals("Copia di {0}", actualCopyOf);
  }

  @Test
  public void getRUStrings() {
    BaseMultilingualDictionary sut = new BaseMultilingualDictionary("ru");
    String actualCopyOf = sut.get(BaseMultilingualString.CopyOf);
    assertEquals("Копия {0}", actualCopyOf);
  }

  @Test
  public void getARStrings() {
    BaseMultilingualDictionary sut = new BaseMultilingualDictionary("ar");
    String actualCopyOf = sut.get(BaseMultilingualString.CopyOf);
    assertEquals("نسخة من {0}", actualCopyOf);
  }

  @Test
  public void getZHStrings() {
    BaseMultilingualDictionary sut = new BaseMultilingualDictionary("zh");
    String actualCopyOf = sut.get(BaseMultilingualString.CopyOf);
    assertEquals("{0}副本", actualCopyOf);
  }

  @Test
  public void getNonExistentLocale() {
    BaseMultilingualDictionary sut = new BaseMultilingualDictionary("12");
    String actualCopyOf = sut.get(BaseMultilingualString.CopyOf);
    assertEquals("Copy of {0}", actualCopyOf);
  }

  @Test
  public void getNullLocale() {
    BaseMultilingualDictionary sut = new BaseMultilingualDictionary(null);
    String actualCopyOf = sut.get(BaseMultilingualString.CopyOf);
    assertEquals("Copy of {0}", actualCopyOf);
  }

  @Test
  public void getEmptyLocale() {
    BaseMultilingualDictionary sut = new BaseMultilingualDictionary("");
    String actualCopyOF = sut.get(BaseMultilingualString.CopyOf);
    assertEquals("Copy of {0}", actualCopyOF);
  }
}
