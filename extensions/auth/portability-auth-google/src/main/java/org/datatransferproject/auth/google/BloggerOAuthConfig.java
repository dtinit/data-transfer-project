/*
 * Copyright 2019 The Data Transfer Project Authors.
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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Map;
import java.util.Set;

/**
 * Class that supplies Google Blogger-specific OAuth2 info, this is needed so because multiple
 * Google services implement the vertical SOCIAL-POSTS
 */
public class BloggerOAuthConfig extends GoogleOAuthConfig {

  // https://developers.google.com/identity/protocols/OAuth2WebServer

  @Override
  public String getServiceName() {
    return "GoogleBlogger";
  }

  // See https://developers.google.com/identity/protocols/googlescopes
  @Override
  public Map<String, Set<String>> getExportScopes() {
    return ImmutableMap.<String, Set<String>>builder()
        .put("SOCIAL-POSTS", ImmutableSet.of("https://www.googleapis.com/auth/blogger.readonly"))
        .build();
  }

  // See https://developers.google.com/identity/protocols/googlescopes
  @Override
  public Map<String, Set<String>> getImportScopes() {
    return ImmutableMap.<String, Set<String>>builder()
        .put("SOCIAL-POSTS", ImmutableSet.of(
            "https://www.googleapis.com/auth/blogger",
            // Any photos associated with the blog are stored in Drive.
            // This permission only grants access to files created by this app
            "https://www.googleapis.com/auth/drive.file"
        ))
        .build();
  }
}