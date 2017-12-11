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
package org.dataportabilityproject.cloud.google;


import static org.dataportabilityproject.shared.Config.Environment.LOCAL;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.Query;
import com.google.cloud.datastore.QueryResults;
import com.google.cloud.datastore.StructuredQuery.PropertyFilter;
import java.io.IOException;
import org.dataportabilityproject.PortabilityFlags;
import org.dataportabilityproject.cloud.interfaces.BucketStore;
import org.dataportabilityproject.cloud.interfaces.CloudFactory;
import org.dataportabilityproject.cloud.interfaces.CryptoKeyManagementSystem;
import org.dataportabilityproject.cloud.interfaces.JobDataCache;
import org.dataportabilityproject.cloud.interfaces.PersistentKeyValueStore;

public final class GoogleCloudFactory implements CloudFactory {

  private final Datastore datastore;
  private final PersistentKeyValueStore persistentKeyValueStore;
  private final CryptoKeyManagementSystem cryptoKeyManagementSystem;
  private final BucketStore bucketStore;

  public GoogleCloudFactory() {
    try {
      this.datastore = DatastoreOptions
          .newBuilder()
          .setProjectId(System.getenv("GOOGLE_PROJECT_ID"))
          .setCredentials(getCredentials())
          .build()
          .getService();
      this.persistentKeyValueStore = new GooglePersistentKeyValueStore(datastore);
      this.cryptoKeyManagementSystem = new GoogleCryptoKeyManagementSystem();
      this.bucketStore = new GoogleBucketStore();
    } catch (CredentialsException e) {
      throw new IllegalStateException("Problem getting credentials to access GCP services", e);
    }
  }

  @Override
  public JobDataCache getJobDataCache(String jobId, String service) {
    return new GoogleJobDataCache(datastore, jobId, service);
  }

  @Override
  public PersistentKeyValueStore getPersistentKeyValueStore() {
    return persistentKeyValueStore;
  }

  @Override
  public CryptoKeyManagementSystem getCryptoKeyManagementSystem() {
    return cryptoKeyManagementSystem;
  }

  @Override
  public BucketStore getBucketStore() {
    return bucketStore;
  }

  @Override
  public void clearJobData(String jobId) {
    QueryResults<Key> results = datastore.run(Query.newKeyQueryBuilder()
        .setKind(GoogleJobDataCache.USER_KEY_KIND)
        .setFilter(PropertyFilter.hasAncestor(
            datastore.newKeyFactory().setKind(GoogleJobDataCache.JOB_KIND).newKey(jobId)))
        .build());
    results.forEachRemaining(datastore::delete);
  }

  static GoogleCredentials getCredentials() throws CredentialsException {
    // TODO: Check whether we are actually running on GCP once we find out how
    boolean isRunningOnGcp = PortabilityFlags.environment() != LOCAL;
    // DO NOT REMOVE this security check! This ensures we are using the correct GCP credentials.
    if (isRunningOnGcp) {
      String credsLocation = System.getenv("GOOGLE_APPLICATION_CREDENTIALS");
      if (!credsLocation.startsWith("/var/secrets/")) {
        String cause = String.format("You are attempting to obtain credentials from somewhere "
            + "other than Kubernetes secrets in prod. You may have accidentally copied creds into"
            + "your image, which we provide as a local debugging mechanism only. See GCP build "
            + "script (config/gcp/build_and_deploy.sh) for more info. Creds location was: %s",
            credsLocation);
        throw new CredentialsException(cause);
      }
      // Note: Tried an extra check via Kubernetes API to verify GOOGLE_APPLICATION_CREDENTIALS
      // is the same as the secret via Kubernetes, but this API did not seem reliable.
      // (io.kubernetes.client.apis.CoreV1Api.listSecretForAllNamespaces)
    }
    try {
      return GoogleCredentials.getApplicationDefault();
    } catch (IOException e) {
      throw new CredentialsException(
          "Problem obtaining credentials via GoogleCredentials.getApplicationDefault()", e);
    }
  }

  static class CredentialsException extends Exception {
    CredentialsException(String message) {
      super(message);
    }

    CredentialsException(String message, Exception cause) {
      super(message, cause);
    }
  }
}
