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

import static org.datatransferproject.types.common.models.DataVertical.CALENDAR;
import static org.datatransferproject.types.common.models.DataVertical.CONTACTS;
import static org.datatransferproject.types.common.models.DataVertical.MAIL;
import static org.datatransferproject.types.common.models.DataVertical.PHOTOS;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Map;
import java.util.Set;
import org.datatransferproject.auth.OAuth2Config;
import org.datatransferproject.types.common.models.DataVertical;

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
  public Map<DataVertical, Set<String>> getExportScopes() {
    return ImmutableMap.<DataVertical, Set<String>>builder()
        .put(MAIL, ImmutableSet.of("user.read", "Mail.Read"))
        .put(CONTACTS, ImmutableSet.of("user.read", "Contacts.Read"))
        .put(CALENDAR, ImmutableSet.of("user.read", "Calendars.Read"))
        .put(PHOTOS, ImmutableSet.of("user.read", "Files.Read"))
        .build();
  }

  @Override
  public Map<DataVertical, Set<String>> getImportScopes() {
    return ImmutableMap.<DataVertical, Set<String>>builder()
        .put(MAIL, ImmutableSet.of("user.read", "Mail.ReadWrite"))
        .put(CONTACTS, ImmutableSet.of("user.read", "Contacts.ReadWrite"))
        .put(CALENDAR, ImmutableSet.of("user.read", "Calendars.ReadWrite"))
        .put(PHOTOS, ImmutableSet.of("user.read", "Files.ReadWrite"))
        .build();
  }
}
