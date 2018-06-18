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
package org.dataportabilityproject.auth.derived;

import org.dataportabilityproject.spi.api.auth.AuthDataGenerator;
import org.dataportabilityproject.spi.api.types.AuthFlowConfiguration;
import org.dataportabilityproject.types.transfer.auth.AuthData;
import org.dataportabilityproject.types.transfer.auth.TokenAuthData;

/**
 * Generates authentication data for the derived data demo importer.
 *
 * <p>Since the demo does not authenticate against a live service, the OAuth url is set set directly
 * to the callback address.
 */
public class DerivedDemoAuthDataGenerator implements AuthDataGenerator {

  @Override
  public AuthFlowConfiguration generateConfiguration(String callbackBaseUrl, String id) {
    return new AuthFlowConfiguration(callbackBaseUrl + "/callback/derived-demo?code=123");
  }

  @Override
  public AuthData generateAuthData(
      String callbackBaseUrl, String authCode, String id, AuthData initialAuthData, String extra) {
    // create a simulated token; since this is a demo extension that does not perform an actual
    // import, OAuth is bypassed.
    return new TokenAuthData("123");
  }
}
