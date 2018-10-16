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

package org.datatransferproject.auth.smugmug;

import com.google.api.client.http.HttpTransport;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.List;
import org.datatransferproject.api.launcher.ExtensionContext;
import org.datatransferproject.auth.OAuth1ServiceExtension;
import org.datatransferproject.spi.api.auth.AuthDataGenerator;
import org.datatransferproject.spi.api.auth.AuthServiceProviderRegistry.AuthMode;
import org.datatransferproject.spi.api.auth.extension.AuthServiceExtension;
import org.datatransferproject.spi.cloud.storage.AppCredentialStore;
import org.datatransferproject.types.transfer.auth.AppCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SmugMugAuthServiceExtension extends OAuth1ServiceExtension {
  public SmugMugAuthServiceExtension() {
    super(new SmugMugOAuthConfig());
  }
}
