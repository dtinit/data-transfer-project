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
package org.datatransferproject.spi.cloud.extension;

import org.datatransferproject.api.launcher.AbstractExtension;
import org.datatransferproject.spi.cloud.storage.AppCredentialStore;
import org.datatransferproject.spi.cloud.storage.JobStore;

/** Cloud extensions implement this interface to be loaded in either a api or transfer worker process. */
public interface CloudExtension extends AbstractExtension {

  /**
   * Returns the extension job store instance.
   *
   * @return the instance
   */
  JobStore getJobStore();

  /**
   * Returns the extension app credential store instance.
   *
   * @return the instance
   */
  AppCredentialStore getAppCredentialStore();
}
