/*
 * Copyright 2018 The Data Transfer Project Authors.
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
package org.datatransferproject.cloud.local;

import org.datatransferproject.api.launcher.ExtensionContext;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.spi.cloud.extension.CloudExtension;
import org.datatransferproject.spi.cloud.storage.AppCredentialStore;
import org.datatransferproject.spi.cloud.storage.JobStore;

/** */
public class LocalCloudExtension implements CloudExtension {

  private Monitor monitor;

  @Override
  public JobStore getJobStore() {
    return new LocalJobStore(monitor);
  }

  @Override
  public AppCredentialStore getAppCredentialStore() {
    // TODO implement
    return new LocalAppCredentialStore();
  }

  @Override
  public void initialize(ExtensionContext context) {
    monitor = context.getMonitor();
  }
}
