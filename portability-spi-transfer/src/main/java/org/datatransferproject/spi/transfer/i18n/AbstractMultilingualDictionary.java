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

import com.google.common.base.Strings;
import java.util.Hashtable;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class AbstractMultilingualDictionary {
  private Hashtable<MultilingualString, String> dict = new Hashtable<>();
  private String bundle;

  public AbstractMultilingualDictionary(
      String locale, String bundle, MultilingualString[] enumValues) {
    this.bundle = bundle;
    if (Strings.isNullOrEmpty(locale)) {
      for (MultilingualString value : enumValues) {
        dict.put(value, value.getDefaultValue());
      }

      return;
    }

    for (MultilingualString value : enumValues) {
      dict.put(value, get(value, Locale.forLanguageTag(locale)));
    }
  }

  public String get(MultilingualString multilingualString) {
    return dict.get(multilingualString);
  }

  private String get(MultilingualString multilingualString, Locale locale) {
    try {
      ResourceBundle resourceBundle = ResourceBundle.getBundle(bundle, locale);
      return resourceBundle.getString(multilingualString.getKey());
    } catch (MissingResourceException e) {
      return multilingualString.getDefaultValue();
    }
  }
}
