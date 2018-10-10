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

package org.datatransferproject.auth.microsoft;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Map;
import org.datatransferproject.auth.oauth2.OAuth2Config;

public class MicrosoftOAuthConfig implements OAuth2Config {

  @Override
  public String getServiceName() {
    return "Microsoft";
  }

  @Override
  public String getAuthUrl() {
    return "https://login.microsoftonline.com/common/oauth2/v2.0/authorize";
  }

  @Override
  public String getTokenUrl() {
    return "https://login.microsoftonline.com/common/oauth2/v2.0/token";
  }

  @Override
  public Map<String, List<String>> getExportScopes() {
    return ImmutableMap.<String, List<String>>builder()
        .put("CALENDAR", ImmutableList.of("user.read", "Calendars.Read"))
        .put("CONTACTS", ImmutableList.of("user.read", "Contacts.Read"))
        .put("MAIL", ImmutableList.of("user.read", "Mail.Read"))
        .put("OFFLINE-DATA", ImmutableList.of("user.read", "Files.Read.All"))
        .build();
  }

  @Override
  public Map<String, List<String>> getImportScopes() {
    return ImmutableMap.<String, List<String>>builder()
        .put("CALENDAR", ImmutableList.of("user.read", "Calendars.ReadWrite"))
        .put("CONTACTS", ImmutableList.of("user.read", "Contacts.ReadWrite"))
        .put("MAIL", ImmutableList.of("user.read", "Mail.ReadWrite"))
        .build();
  }
}
