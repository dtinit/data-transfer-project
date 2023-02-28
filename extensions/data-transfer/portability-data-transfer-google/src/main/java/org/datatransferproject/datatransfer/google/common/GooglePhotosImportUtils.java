/*
 * Copyright 2022 The Data Transfer Project Authors.
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
package org.datatransferproject.datatransfer.google.common;

import com.google.common.base.Strings;

// TODO(#1144) consider porting this logic to a transmogrify config (and thus deleting this class).
public class GooglePhotosImportUtils {

  public static String cleanAlbumTitle(String originalTitle) {
    String title = Strings.nullToEmpty(originalTitle);

    // Album titles are restricted to 500 characters
    // https://developers.google.com/photos/library/guides/manage-albums#creating-new-album
    if (title.length() > 500) {
      title = title.substring(0, 497) + "...";
    }
    return title;
  }

  public static String cleanDescription(String originalDescription) {
    String description = Strings.isNullOrEmpty(originalDescription) ? "" : originalDescription;

    // Descriptions are restricted to 1000 characters
    // https://developers.google.com/photos/library/guides/upload-media#creating-media-item
    if (description.length() > 1000) {
      description = description.substring(0, 997) + "...";
    }
    return description;
  }
}
