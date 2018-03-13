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
package org.dataportabilityproject.cloud.local;

import org.dataportabilityproject.api.launcher.ExtensionContext;
import org.dataportabilityproject.spi.cloud.extension.CloudExtension;
import org.dataportabilityproject.spi.cloud.storage.BucketStore;
import org.dataportabilityproject.spi.cloud.storage.CryptoKeyStore;
import org.dataportabilityproject.spi.cloud.storage.JobStore;

/** */
public class LocalCloudExtension implements CloudExtension {

  @Override
  public JobStore getJobStore() {
    return new LocalJobStore();
  }

  @Override
  public BucketStore getBucketStore() {
    // TODO implement
    return null;
  }

  @Override
  public CryptoKeyStore getCryptoKeyStore() {
    // TODO implement
    return null;
  }

  @Override
  public void initialize(ExtensionContext context) {}
}
