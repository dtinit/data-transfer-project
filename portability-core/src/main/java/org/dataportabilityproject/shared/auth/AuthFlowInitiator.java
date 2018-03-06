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
import javax.annotation.Nullable;
import org.dataportabilityproject.types.transfer.auth.AuthData;

/** Represents the authUrl, and optional state, to request authorization for a service. */
@AutoValue
public abstract class AuthFlowInitiator {

  public static AuthFlowInitiator create(String url) {
    return create(url, null);
  }

  public static AuthFlowInitiator create(String url, @Nullable AuthData initialAuthData) {
    return new AutoValue_AuthFlowInitiator(url, initialAuthData);
  }

  public abstract String authUrl();

  @Nullable
  public abstract AuthData initialAuthData();
}
