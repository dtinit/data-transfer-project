/*
 * Copyright 2017 Google Inc.
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
package org.dataportabilityproject.datatransfer.google.common;

import static org.dataportabilityproject.shared.PortableDataType.CALENDAR;
import static org.dataportabilityproject.shared.PortableDataType.CONTACTS;
import static org.dataportabilityproject.shared.PortableDataType.MAIL;
import static org.dataportabilityproject.shared.PortableDataType.PHOTOS;
import static org.dataportabilityproject.shared.PortableDataType.TASKS;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.DataStoreFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.people.v1.PeopleServiceScopes;
import com.google.api.services.tasks.TasksScopes;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ListMultimap;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.dataportabilityproject.shared.PortableDataType;
import org.dataportabilityproject.shared.ServiceMode;

/**
 * Static objects shared with all Google services.
 */
public final class GoogleStaticObjects {
  /**
   * Global instance of the JSON factory.
   */
  public static final JsonFactory JSON_FACTORY = new JacksonFactory();
  public static final String APP_NAME = "Portability";
  private static final java.io.File DATA_STORE_DIR =
      new java.io.File(System.getProperty("user.home"), ".store/google_creds");

  /**
   * Global instance of the HTTP transport.
   */
  private static HttpTransport HTTP_TRANSPORT;
  /**
   * Global instance of the {@link DataStoreFactory}. The best practice is to make it a single
   * globally shared instance across your application.
   */
  private static FileDataStoreFactory DATA_STORE_FACTORY;

  static {
    try {
      HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
      DATA_STORE_FACTORY = new FileDataStoreFactory(DATA_STORE_DIR);
    } catch (Throwable t) {
      t.printStackTrace();
      System.exit(1);
    }
  }

  private GoogleStaticObjects() {
  }

  public static HttpTransport getHttpTransport() {
    return HTTP_TRANSPORT;
  }

  public static FileDataStoreFactory getDataStoreFactory() {
    return DATA_STORE_FACTORY;
  }

  /**
   * Static values used in converters
   */
  // TODO(olsona): find optimum value
  public static final int MAX_ATTENDEES = 100;
}
