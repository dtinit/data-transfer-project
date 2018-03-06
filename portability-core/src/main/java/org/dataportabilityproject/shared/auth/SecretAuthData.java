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
package org.dataportabilityproject.shared.auth;

import com.google.auto.value.AutoValue;
import org.dataportabilityproject.types.transfer.auth.AuthData;

/** A simple implimentation of {@link AuthData} that contains just a secret. */
@AutoValue
public abstract class SecretAuthData extends AuthData {

  public static SecretAuthData create(String secret) {
    return new AutoValue_SecretAuthData(secret);
  }

  public abstract String secret();
}
