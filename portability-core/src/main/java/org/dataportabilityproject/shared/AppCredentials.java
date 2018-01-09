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

import com.google.auto.value.AutoValue;

/**
 * Holder for an application key and secret.
 */
@AutoValue
public abstract class AppCredentials {
    public abstract String key();
    public abstract String secret();

    public static AppCredentials create(String key, String secret) {
        return new AutoValue_AppCredentials(key, secret);
    }
}
