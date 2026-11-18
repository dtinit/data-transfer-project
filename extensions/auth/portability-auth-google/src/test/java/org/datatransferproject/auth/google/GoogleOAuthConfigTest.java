/*
 * Copyright 2026 The Data Transfer Project Authors.
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

package org.datatransferproject.auth.google;

import static com.google.common.truth.Truth.assertThat;

import org.datatransferproject.types.common.models.DataVertical;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class GoogleOAuthConfigTest {

  private static final String PHOTOS_APPEND_ONLY =
      "https://www.googleapis.com/auth/photoslibrary.appendonly";
  private static final String PHOTOS_READ_ONLY =
      "https://www.googleapis.com/auth/photoslibrary.readonly";

  private final GoogleOAuthConfig config = new GoogleOAuthConfig();

  @ParameterizedTest
  @EnumSource(
      value = DataVertical.class,
      names = {"PHOTOS", "VIDEOS", "MEDIA"})
  void googlePhotosImport_asksOnlyToAddToTheLibrary(DataVertical vertical) {
    assertThat(config.getImportScopes().get(vertical)).containsExactly(PHOTOS_APPEND_ONLY);
  }

  @ParameterizedTest
  @EnumSource(
      value = DataVertical.class,
      names = {"PHOTOS", "VIDEOS", "MEDIA"})
  void googlePhotosExport_asksOnlyToReadTheLibrary(DataVertical vertical) {
    assertThat(config.getExportScopes().get(vertical)).containsExactly(PHOTOS_READ_ONLY);
  }
}
