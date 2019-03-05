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

package org.datatransferproject.auth.google;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Map;
import java.util.Set;
import org.datatransferproject.auth.OAuth2Config;

/**
 * Class that supplies Google-specific OAuth2 info
 */
public class GoogleOAuthConfig implements OAuth2Config {

  // https://developers.google.com/identity/protocols/OAuth2WebServer

  @Override
  public String getServiceName() {
    return "Google";
  }

  // See https://developers.google.com/identity/protocols/OAuth2WebServer#creatingclient
  @Override
  public String getAuthUrl() {
    return "https://accounts.google.com/o/oauth2/auth";
  }

  //See https://developers.google.com/identity/protocols/OAuth2WebServer#exchange-authorization-code
  @Override
  public String getTokenUrl() {
    return "https://www.googleapis.com/oauth2/v4/token";
  }

  // See https://developers.google.com/identity/protocols/googlescopes
  @Override
  public Map<String, Set<String>> getExportScopes() {
    return ImmutableMap.<String, Set<String>>builder()
        .put("BLOBS", ImmutableSet.of("https://www.googleapis.com/auth/drive.readonly"))
        .put("CALENDAR", ImmutableSet.of("https://www.googleapis.com/auth/calendar.readonly"))
        .put("CONTACTS", ImmutableSet.of("https://www.googleapis.com/auth/contacts.readonly"))
        .put("MAIL", ImmutableSet.of("https://www.googleapis.com/auth/gmail.readonly"))
        .put("PHOTOS", ImmutableSet.of("https://www.googleapis.com/auth/photoslibrary.readonly"))
        // For G+
        .put("SOCIAL-POSTS", ImmutableSet.of("https://www.googleapis.com/auth/plus.login"))
        .put("TASKS", ImmutableSet.of("https://www.googleapis.com/auth/tasks.readonly"))
        .build();
  }

  // See https://developers.google.com/identity/protocols/googlescopes
  @Override
  public Map<String, Set<String>> getImportScopes() {
    return ImmutableMap.<String, Set<String>>builder()
        .put("BLOBS", ImmutableSet.of("https://www.googleapis.com/auth/drive"))
        .put("CALENDAR", ImmutableSet.of("https://www.googleapis.com/auth/calendar"))
        .put("CONTACTS", ImmutableSet.of("https://www.googleapis.com/auth/contacts"))
        .put("MAIL", ImmutableSet.of("https://www.googleapis.com/auth/gmail.modify"))
        .put("PHOTOS", ImmutableSet.of("https://www.googleapis.com/auth/photoslibrary.appendonly"))
        .put("TASKS", ImmutableSet.of("https://www.googleapis.com/auth/tasks"))
        .build();
  }

  @Override
  public Map<String, String> getAdditionalAuthUrlParameters() {
    return ImmutableMap.of("approval_prompt", "force");
  }
}
