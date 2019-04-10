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
package org.datatransferproject.api.launcher;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

/** Provides versioning information to the runtime. */
public class Version {
  private static final String UNKNOWN = "unknown";
  private static final String VERSION;
  private static final String HASH;

  public static String getVersion() {
    return VERSION;
  }

  public static String getSourceHash() {
    return HASH;
  }

  private Version() {}

  static {
    Properties properties = new Properties();
    URL url = Version.class.getResource("/META-INF/launcher.properties");
    if (url == null) {
      System.err.println("Launcher version file (launcher.properties) not found");
    } else {
      try (final InputStream stream = url.openStream()) {
        properties.load(stream);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    VERSION = properties.getProperty("version", UNKNOWN);
    HASH = properties.getProperty("hash", UNKNOWN);
  }
}
