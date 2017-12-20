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
package org.dataportabilityproject.shared.local;

import static com.google.gdata.util.common.base.Preconditions.checkState;

import com.google.common.collect.ImmutableMap;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import org.dataportabilityproject.PortabilityFlags;
import org.dataportabilityproject.shared.Config.Environment;
import org.dataportabilityproject.shared.LogUtils;

/**
 * Looks up app secrets from a local secrets.csv file rather than pulling from the cloud.
 *
 * <p>Should only be used for local development.
 */
public class LocalSecrets {
  private static final LocalSecrets INSTANCE = new LocalSecrets();
  private static final String SECRETS_FILENAME = "secrets.csv";

  private final ImmutableMap<String, String> secrets;

  public static LocalSecrets getInstance() {
    return INSTANCE;
  }

  public String get(String key) {
    return secrets.get(key);
  }

  private LocalSecrets() {
    // DO NOT REMOVE this security check! For prod, we should read secrets from the cloud.
    checkState(PortabilityFlags.environment() == Environment.LOCAL,
        "Secrets from secrets.csv is only supported for local development");
    ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
    try {
      BufferedReader reader =
          new BufferedReader(new InputStreamReader(getStream(SECRETS_FILENAME)));
      String line;
      while ((line = reader.readLine()) != null) {
        // allow for comments
        String trimmedLine = line.trim();
        if (!trimmedLine.startsWith("//") && !trimmedLine.startsWith("#")
            && !trimmedLine.isEmpty()) {
          String[] parts = trimmedLine.split(",");
          checkState(parts.length == 2,
              "Each line should have exactly 2 string seperated by a ,: %s", line);
          builder.put(parts[0].trim(), parts[1].trim());
        }
      }
    } catch (IOException e) {
      LogUtils.log("Fatal: Problem parsing secrets file %s: %s", SECRETS_FILENAME, e);
      System.exit(1);
    }
    this.secrets = builder.build();
  }

  /** Reads the path as a stream from file system or jar. */
  private static InputStream getStream(String filePath) throws IOException {
    InputStream in = LocalSecrets.class.getClassLoader().getResourceAsStream(filePath);
    if (in == null) {
      throw new IOException("Could not create input stream, filePath: " + filePath);
    }
    return in;
  }
}
