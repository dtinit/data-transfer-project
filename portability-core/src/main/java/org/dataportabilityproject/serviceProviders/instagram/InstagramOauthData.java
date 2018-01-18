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
package org.dataportabilityproject.serviceProviders.instagram;

import com.google.auto.value.AutoValue;
import javax.annotation.Nullable;
import org.dataportabilityproject.shared.auth.AuthData;

@AutoValue
abstract class InstagramOauthData extends AuthData {

  static InstagramOauthData create(String accessToken,
      String refreshToken,
      String tokenServerEncodedUrl) {
    return new AutoValue_InstagramOauthData(accessToken, refreshToken, tokenServerEncodedUrl);
  }

  abstract String accessToken();

  @Nullable
  abstract String refreshToken(); // TODO: Determine if we can get refresh token in non-Sandbox mode

  abstract String tokenServerEncodedUrl();
}
