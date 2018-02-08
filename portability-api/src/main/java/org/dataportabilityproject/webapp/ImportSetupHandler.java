/*
* Copyright 2017 The Data-Portability Project Authors.
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
package org.dataportabilityproject.webapp;

import com.google.inject.Inject;
import com.sun.net.httpserver.HttpHandler;
import org.dataportabilityproject.ServiceProviderRegistry;
import org.dataportabilityproject.cloud.interfaces.CloudFactory;
import org.dataportabilityproject.job.TokenManager;
import org.dataportabilityproject.shared.settings.CommonSettings;

/**
 * {@link HttpHandler} that handles starting a import job.
 */
final class ImportSetupHandler extends SetupHandler {

  static final String PATH = "/_/importSetup";

  @Inject
  ImportSetupHandler(
      ServiceProviderRegistry serviceProviderRegistry,
      CloudFactory cloudFactory,
      CommonSettings commonSettings,
      TokenManager tokenManager) {
    super(serviceProviderRegistry, cloudFactory, commonSettings, Mode.IMPORT, PATH, tokenManager);
  }
}
