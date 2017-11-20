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

import com.google.common.base.Preconditions;
import java.io.IOException;
import org.dataportabilityproject.shared.Secrets;

public final class OnlinePasswordAuthDataGenerator implements OnlineAuthDataGenerator {

  @Override
  public AuthFlowInitiator generateAuthUrl(String id) throws IOException {
    return AuthFlowInitiator.create(Secrets.getInstance().baseUrl() + "/simplelogin");
  }

  @Override
  public AuthData generateAuthData(String authCode, String id, AuthData initialAuthData, String extra)
      throws IOException {
    Preconditions.checkArgument(initialAuthData == null, "initial auth data not expected");
    return PasswordAuthData.create(authCode, extra);
  }
}
