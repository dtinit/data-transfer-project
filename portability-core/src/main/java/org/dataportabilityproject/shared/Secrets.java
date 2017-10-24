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
package org.dataportabilityproject.shared;

import static com.google.common.base.Preconditions.checkState;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Files;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.List;

/**
 * Holds Api keys and secrets for the various services.
 */
public final class Secrets {
    private final ImmutableMap<String, String> secrets;

    public Secrets(String filePath) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(getStream(filePath)));
        ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
        String line;
        while ((line = reader.readLine()) != null) {
            // allow for comments
            String trimmedLine = line.trim();
            if (!trimmedLine.startsWith("//") && !trimmedLine.startsWith("#") && !trimmedLine.isEmpty()) {
                String[] parts = trimmedLine.split(",");
                checkState(parts.length == 2, "Each line should have exactly 2 string seperated by a ,: %s", line);
                builder.put(parts[0].trim(), parts[1].trim());
            }
        }
        this.secrets = builder.build();
    }

    public String get(String key) {
        return secrets.get(key);
    }

    /**
     * Looks up a given key in the secrets file, then uses that value to open a secondary resource
     * file and streams that out.
     **/
    public InputStream getReferencedInputStream(String key) throws IOException {
        String path = secrets.get(key);
        checkState(!Strings.isNullOrEmpty(path), "Key %s was not defined in secrets file", key);
        return getStream(path);
    }

    /** Reads the path as a stream from file system or jar. */
    private InputStream getStream(String filePath) throws IOException {
        InputStream in = getClass().getClassLoader().getResourceAsStream(filePath);
        if (in == null) {
            throw new IOException("Could not create input stream, filePath: " + filePath);
        }
        return in;
    }
}
