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

/**
 * A simple implimentation of {@link AuthData} that contains a token and a secret.
 */
@AutoValue
public abstract class TokenSecretAuthData extends AuthData {

  public static TokenSecretAuthData create(String token, String secret) {
    return new AutoValue_TokenSecretAuthData(token, secret);
  }

  public abstract String token();

  public abstract String secret();
}
