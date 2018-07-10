/*
 * Copyright 2018 The Data Transfer Project Authors.
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

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.DataStoreFactory;
import com.google.api.client.util.store.FileDataStoreFactory;

/**
 * Static objects shared with all Google services.
 */
public final class GoogleStaticObjects {

  public static final String APP_NAME = "Portability";

  /**
   * Static values used in calendar converters
   */
  // TODO(olsona): find optimum value
  public static final int MAX_ATTENDEES = 100;
  public static final String CALENDAR_TOKEN_PREFIX = "calendar:";
  public static final String EVENT_TOKEN_PREFIX = "event:";

  /**
   * Static values used in contacts converters
   */
  public static final String SELF_RESOURCE = "people/me";

  // List of all fields we want to get from the Google Contacts API
  // NB: this will have to be updated as we support more fields
  public static final String PERSON_FIELDS = "emailAddresses,names,phoneNumbers,addresses";

  public static final int VCARD_PRIMARY_PREF = 1;
  public static final String SOURCE_PARAM_NAME_TYPE = "Source_type";
  public static final String CONTACT_SOURCE_TYPE = "CONTACT";
}
