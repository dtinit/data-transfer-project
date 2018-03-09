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
package org.dataportabilityproject.cloud.google;

import com.google.common.base.Preconditions;
import org.dataportabilityproject.api.launcher.ExtensionContext;
import org.dataportabilityproject.spi.cloud.extension.CloudExtension;
import org.dataportabilityproject.spi.cloud.extension.CloudExtensionModule;

/**
 * {@link CloudExtension} for Google Cloud Platform.
 */
public class GoogleCloudExtension implements CloudExtension {
  private CloudExtensionModule cloudExtensionModule;
  private boolean initialized = false;

  // TODO(seehamrun): Needed for ServiceLoader?
  public GoogleCloudExtension() {}

  /*
   * Initializes the GoogleCloudExtension based on the ExtensionContext.
   *
   * The ExtensionContext should provide the following:
   * <li> HttpTransport
   * <li> JsonFactory
   */
  @Override
  public void initialize(ExtensionContext context) {
    Preconditions.checkArgument(
        !initialized, "Attempting to initialize GoogleCloudExtension more than once");
    cloudExtensionModule = new GoogleCloudModule(context);
    initialized = true;
  }

  @Override
  public void shutdown() {
    this.initialized = false;
  }

  @Override
  public CloudExtensionModule getCloudModule() {
    return cloudExtensionModule;
  }
}
