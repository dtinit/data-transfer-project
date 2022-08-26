/*
 * Copyright 2018 The Data Transfer Project Authors.
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

package org.datatransferproject.auth.smugmug;

import static org.datatransferproject.types.common.models.DataVertical.PHOTOS;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.datatransferproject.auth.OAuth1Config;
import org.datatransferproject.spi.api.auth.AuthServiceProviderRegistry.AuthMode;
import org.datatransferproject.types.common.models.DataVertical;

/**
 * Class that supplies SmugMug-specific OAuth1 info
 * See https://api.smugmug.com/api/v2/doc/tutorial/authorization.html
 */
public class SmugMugOAuthConfig implements OAuth1Config {

  private static final String ACCESS = "Access";
  private static final String PERMISSIONS = "Permissions";

  @Override
  public String getServiceName() {
    return "SmugMug";
  }

  @Override
  public String getRequestTokenUrl() {
    return "https://secure.smugmug.com/services/oauth/1.0a/getRequestToken";
  }

  @Override
  public String getAuthorizationUrl() {
    return "https://secure.smugmug.com/services/oauth/1.0a/authorize";
  }

  @Override
  public String getAccessTokenUrl() {
    return "https://secure.smugmug.com/services/oauth/1.0a/getAccessToken";
  }

  @Override
  public List<DataVertical> getExportTypes() {
    return ImmutableList.of(PHOTOS);
  }

  @Override
  public List<DataVertical> getImportTypes() {
    return ImmutableList.of(PHOTOS);
  }

  public Map<String, String> getAdditionalUrlParameters(
      DataVertical dataType, AuthMode mode, OAuth1Step step) {

    if (dataType.equals(PHOTOS) && step == OAuth1Step.AUTHORIZATION) {
      return (mode == AuthMode.EXPORT)
          ? ImmutableMap.of(ACCESS, "Full", PERMISSIONS, "Read")
          : ImmutableMap.of(ACCESS, "Full", PERMISSIONS, "Add");
    }

    // default
    return Collections.emptyMap();
  }
}
