/*
 * Copyright 2018 The Data-Portability Project Authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.dataportabilityproject.spi.cloud.storage;

/**
 * Object storage in buckets.
 *
 * This class is intended to be implemented by extensions that support storage in various back-end
 * services.
 */
public interface BucketStore {
  // Get an app credential (i.e. app key or secret). Each implementation may have its own convention
  // for where/how app credential data is stored.
  byte[] getAppCredentialBlob(String blobName);
}
