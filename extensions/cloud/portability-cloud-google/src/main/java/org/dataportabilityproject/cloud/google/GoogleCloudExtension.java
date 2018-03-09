/*
 * Copyright 2018 The Data-Portability Project Authors.
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

import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.datastore.Datastore;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import org.dataportabilityproject.api.launcher.ExtensionContext;
import org.dataportabilityproject.spi.cloud.extension.CloudExtension;
import org.dataportabilityproject.spi.cloud.storage.BucketStore;
import org.dataportabilityproject.spi.cloud.storage.CryptoKeyStore;
import org.dataportabilityproject.spi.cloud.storage.JobStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GoogleCloudExtension implements CloudExtension {
  private static final Logger logger = LoggerFactory.getLogger(GoogleCloudExtension.class);
  private GoogleCredentials googleCredentials;
  private HttpTransport transport;
  private JsonFactory jsonFactory;
  private Datastore datastore;
  private JobStore jobstore;
  private BucketStore bucketStore;
  private GoogleCryptoKeyStore cryptoKeyManagementSystem;
  private String projectId;
  private boolean initialized = false;

  @Override
  public JobStore getJobStore() {
    Preconditions.checkArgument(initialized, "Attempting to getJobStore() before initializing");
    return jobstore;
  }

  @Override
  public BucketStore getBucketStore() {
    Preconditions.checkArgument(initialized, "Attempting to getBucketStore() before initializing");
    return bucketStore;
  }

  @Override
  public CryptoKeyStore getCryptoKeyStore() {
    Preconditions.checkArgument(
        initialized, "Attempting to getCryptoKeyStore() before initializing");
    return cryptoKeyManagementSystem;
  }

  /*
   * Initializes the GoogleCloudExtension based on the ExtensionContext.
   *
   * The ExtensionContext should provide the following:
   * <li> Google Project Id
   * <li> GoogleCredentials
   * <li> DataStore
   * <li> HttpTransport
   * <li> JsonFactory
   */
  @Override
  public void initialize(ExtensionContext context) {
    Preconditions.checkArgument(
        !initialized, "Attempting to initialize GoogleCloudExtension more than once");
    validateContext(context);

    try {
      jobstore = new GoogleJobStore(datastore);
      bucketStore = new GoogleBucketStore(googleCredentials, projectId);
      cryptoKeyManagementSystem = new GoogleCryptoKeyStore(transport, jsonFactory, projectId);

      initialized = true;
    } catch (GoogleCredentialException e) {
      // TODO: the method doesn't throw an exception, how do we pass this onto the user?
      logger.warn("Error initializing extension: " + this.getClass().getName(), e);
      initialized = false;
    }
  }

  @Override
  public void shutdown() {
    this.initialized = false;
  }

  /* validates the provided context and initializes services needed. */
  private void validateContext(ExtensionContext context) {
    projectId = context.getConfiguration("GOOGLE_PROJECT_ID", "");
    Preconditions.checkArgument(
        !Strings.isNullOrEmpty(projectId), "Google Project Id not found in ExtensionContext");

    googleCredentials = context.getService(GoogleCredentials.class);
    Preconditions.checkArgument(
        googleCredentials != null, "GoogleCredentials not found in ExtensionContext");

    datastore = context.getService(Datastore.class);
    Preconditions.checkArgument(datastore != null, "DataStore not found in ExtensionContext");

    transport = context.getService(HttpTransport.class);
    Preconditions.checkArgument(transport != null, "HttpTransport not found in ExtensionContext");

    jsonFactory = context.getService(JsonFactory.class);
    Preconditions.checkArgument(jsonFactory != null, "JsonFactory not found in ExtensionContext");
  }
}
