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

package org.dataportabilityproject.auth.rememberthemilk;

import org.dataportabilityproject.spi.gateway.auth.AuthDataGenerator;
import org.dataportabilityproject.spi.gateway.types.AuthFlowConfiguration;
import org.dataportabilityproject.types.transfer.auth.AppCredentials;
import org.dataportabilityproject.types.transfer.auth.AuthData;

public class RememberTheMilkAuthDataGenerator implements AuthDataGenerator {

  public RememberTheMilkAuthDataGenerator(AppCredentials appCredentials) {

  }

  @Override
  public AuthFlowConfiguration generateConfiguration(String callbackBaseUrl, String id) {
    return null;
  }

  @Override
  public AuthData generateAuthData(String callbackBaseUrl, String authCode, String id,
      AuthData initialAuthData, String extra) {
    return null;
  }
}
