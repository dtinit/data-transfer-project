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
 * Create a new temp file (e.g. Test.java) in Intellij.
 * Inside the file, enter Alt-Insert -> Copyright
 * It should prompt you to select the new Copyright profile
 * Test out another new test file - the copyright should be imported automatically (note: it might be collapsed so not immediately obvious)
 */

package org.dataportabilityproject.worker;

import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import org.dataportabilityproject.api.launcher.ExtensionContext;
import org.dataportabilityproject.api.launcher.Logger;

/**
 * {@link ExtensionContext} used by the worker.
 */
final class WorkerExtensionContext implements ExtensionContext {
  private static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();
  private static final JsonFactory JSON_FACTORY = new JacksonFactory();

  @Override
  public Logger getLogger() {
    return null;
  }

  @Override
  public <T> T getService(Class<T> type) {
    if (type.equals(HttpTransport.class)) {
      return (T) HTTP_TRANSPORT;
    }
    if (type.equals(JsonFactory.class)) {
      return (T) JSON_FACTORY;
    }
    return null;
  }

  @Override
  public <T> T getConfiguration(String key, T defaultValue) {
    // TODO parse flags. Returning hardcoded local+GOOGLE config now for testing.
    if (key.equals("cloud")) {
      return (T) "GOOGLE";
    }
    return defaultValue;
  }
}
