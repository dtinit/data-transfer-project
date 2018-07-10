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
package org.datatransferproject.transport.jdk.http;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.sun.net.httpserver.HttpHandler;
import org.datatransferproject.api.launcher.TypeManager;
import org.datatransferproject.spi.api.token.TokenManager;
import org.datatransferproject.security.SymmetricKeyGenerator;
import org.datatransferproject.spi.cloud.storage.JobStore;
import org.datatransferproject.spi.api.auth.AuthServiceProviderRegistry;

/** {@link HttpHandler} that handles starting a import job. */
final class ImportSetupHandler extends SetupHandler {

  static final String PATH = "/_/importSetup";

  @Inject
  ImportSetupHandler(
      AuthServiceProviderRegistry registry,
      JobStore store,
      SymmetricKeyGenerator symmetricKeyGenerator,
      TokenManager tokenManager,
      TypeManager typeManager,
      @Named("baseApiUrl") String baseApiUrl) {
    super(
        registry,
        store,
        symmetricKeyGenerator,
        Mode.IMPORT,
        PATH,
        tokenManager,
        typeManager,
        baseApiUrl);
  }
}
