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

package org.datatransferproject.datatransfer.apple.photos;

import org.jetbrains.annotations.NotNull;

public class TestConstants {
  public static final String IMPORT_FOLDER_NAME_BASE = "Imported From ";

  public static final String IMPORT_ZONE_PREFIX = "IMPORT:";

  public static String getImportZoneName(@NotNull String importId) {
    return IMPORT_ZONE_PREFIX + importId;
  }
}
