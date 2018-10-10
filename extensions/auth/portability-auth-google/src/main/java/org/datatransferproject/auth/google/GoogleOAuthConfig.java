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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Map;
import org.datatransferproject.auth.oauth2.OAuth2Config;

public class GoogleOAuthConfig implements OAuth2Config {

  @Override
  public String getServiceName() {
    return "Google";
  }

  @Override
  public String getAuthUrl() {
    return "https://accounts.google.com/o/oauth2/v2/auth";
  }

  @Override
  public String getTokenUrl() {
    return "https://www.googleapis.com/oauth2/v4/token";
  }

  @Override
  public Map<String, List<String>> getExportScopes() {
    return ImmutableMap.<String, List<String>>builder()
        .put("CALENDAR", ImmutableList.of("https://www.googleapis.com/auth/calendar.readonly"))
        .put("CONTACTS", ImmutableList.of("https://www.googleapis.com/auth/contacts.readonly"))
        .put("MAIL", ImmutableList.of("https://www.googleapis.com/auth/gmail.readonly"))
        .put("PHOTOS", ImmutableList.of("https://www.googleapis.com/auth/photoslibrary.readonly"))
        .put("TASKS", ImmutableList.of("https://www.googleapis.com/auth/tasks.readonly"))
        .build();
  }

  @Override
  public Map<String, List<String>> getImportScopes() {
    return ImmutableMap.<String, List<String>>builder()
        .put("CALENDAR", ImmutableList.of("https://www.googleapis.com/auth/calendar"))
        .put("CONTACTS", ImmutableList.of("https://www.googleapis.com/auth/contacts"))
        .put("MAIL", ImmutableList.of("https://www.googleapis.com/auth/gmail.modify"))
        .put("PHOTOS", ImmutableList.of("https://www.googleapis.com/auth/photoslibrary"))
        .put("TASKS", ImmutableList.of("https://www.googleapis.com/auth/tasks"))
        .build();
  }
}
