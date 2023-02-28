/*
 * Copyright 2020 The Data Transfer Project Authors.
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

package org.datatransferproject.auth.koofr;

import static org.datatransferproject.types.common.models.DataVertical.PHOTOS;
import static org.datatransferproject.types.common.models.DataVertical.VIDEOS;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Map;
import java.util.Set;
import org.datatransferproject.auth.OAuth2Config;
import org.datatransferproject.types.common.models.DataVertical;

public class KoofrOAuthConfig implements OAuth2Config {

  @Override
  public String getServiceName() {
    return "Koofr";
  }

  @Override
  public String getAuthUrl() {
    return "https://app.koofr.net/oauth2/auth";
  }

  @Override
  public String getTokenUrl() {
    return "https://app.koofr.net/oauth2/token";
  }

  @Override
  public Map<DataVertical, Set<String>> getExportScopes() {
    // NOTE: KoofrTransferExtension does not implement export at the moment
    return ImmutableMap.<DataVertical, Set<String>>builder()
        .put(PHOTOS, ImmutableSet.of("files.read"))
        .put(VIDEOS, ImmutableSet.of("files.read"))
        .build();
  }

  @Override
  public Map<DataVertical, Set<String>> getImportScopes() {
    return ImmutableMap.<DataVertical, Set<String>>builder()
        .put(PHOTOS, ImmutableSet.of("files.import"))
        .put(VIDEOS, ImmutableSet.of("files.import"))
        .build();
  }
}
