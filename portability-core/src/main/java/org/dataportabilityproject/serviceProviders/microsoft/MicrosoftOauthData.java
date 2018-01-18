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
package org.dataportabilityproject.serviceProviders.microsoft;

import com.google.auto.value.AutoValue;
import org.dataportabilityproject.shared.auth.AuthData;

@AutoValue
abstract class MicrosoftOauthData extends AuthData {

  static MicrosoftOauthData create(String accessToken,
      String refreshToken,
      String tokenServerEncodedUrl,
      String accountAddress) {
    return new AutoValue_MicrosoftOauthData(accessToken, refreshToken, tokenServerEncodedUrl,
        accountAddress);
  }

  abstract String accessToken();

  abstract String refreshToken();

  abstract String tokenServerEncodedUrl();

  abstract String accountAddress(); // TODO: remove if not needed
}
