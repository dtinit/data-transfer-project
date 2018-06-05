/*
 * Copyright 2018 The Data Transfer Project Authors.
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

package org.dataportabilityproject.cloud.google;

import static com.google.common.base.Preconditions.checkState;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.dataportabilityproject.cloud.google.GoogleCloudExtensionModule.ProjectId;
import org.dataportabilityproject.spi.cloud.storage.AppCredentialStore;
import org.dataportabilityproject.types.transfer.auth.AppCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * App credential storage using Google Cloud Platform.
 *
 * <p>App keys are stored raw in Google Cloud Storage. App secrets are encrypted using Cloud KMS by
 * a project admin (using encrypt_and_upload_secrets.sh). This class returns {@link AppCredentials}
 * created by looking up keys and secrets in GCS, and decrypting the secrets with KMS.
 */
@Singleton
final class GoogleAppCredentialStore implements AppCredentialStore {
  private static final Logger logger = LoggerFactory.getLogger(AppCredentialStore.class);
  private static final Integer CACHE_EXPIRATION_MINUTES = 10;
  private static final String APP_CREDENTIAL_BUCKET_PREFIX = "app-data-";
  private static final String KEYS_DIR = "keys/";
  private static final String KEY_EXTENSION = ".txt";
  private static final String SECRETS_DIR = "encrypted_secrets/";
  private static final String SECRET_EXTENSION = ".encrypted";

  private final GoogleAppSecretDecrypter appSecretDecrypter;
  private final Storage storage;
  private final String bucketName;

  /**
   * Store keys and secrets in a cache so we can reduce load on cloud storage / KMS when reading
   * keys and reading/decrypting secrets several times on startup.
   *
   * <p>Set the cache to reload keys/secrets periodically so that in the event of a key/secret being
   * compromised, we can update them without restarting our servers.
   */
  private final LoadingCache<String, String> keys;
  private final LoadingCache<String, String> secrets;

  @Inject
  GoogleAppCredentialStore(
      GoogleAppSecretDecrypter appSecretDecrypter,
      GoogleCredentials googleCredentials,
      @ProjectId String projectId) {
    this.appSecretDecrypter = appSecretDecrypter;
    this.storage =
        StorageOptions.newBuilder()
            .setProjectId(projectId)
            .setCredentials(googleCredentials)
            .build()
            .getService();
    // Google Cloud Platform requires bucket names be unique across projects, so we include project
    // ID in the bucket name.
    this.bucketName = APP_CREDENTIAL_BUCKET_PREFIX + projectId;
    this.keys =
        CacheBuilder.newBuilder()
            .expireAfterWrite(CACHE_EXPIRATION_MINUTES, TimeUnit.MINUTES)
            .build(
                new CacheLoader<String, String>() {
                  @Override
                  public String load(String key) throws Exception {
                    return lookupKey(key);
                  }
                });
    this.secrets =
        CacheBuilder.newBuilder()
            .expireAfterWrite(CACHE_EXPIRATION_MINUTES, TimeUnit.MINUTES)
            .build(
                new CacheLoader<String, String>() {
                  @Override
                  public String load(String key) throws Exception {
                    return lookupSecret(key);
                  }
                });
  }

  public AppCredentials getAppCredentials(String keyName, String secretName) throws IOException {
    String key;
    String secret;
    try {
      key = keys.get(keyName);
    } catch (ExecutionException e) {
      throw new IOException("Couldn't lookup key: " + keyName, e);
    }

    try {
      secret = secrets.get(secretName);
    } catch (ExecutionException e) {
      throw new IOException("Couldn't lookup secret: " + secretName, e);
    }

    return new AppCredentials(key, secret);
  }

  private byte[] getRawBytes(String blobName) {
    Bucket bucket = storage.get(bucketName);
    Preconditions.checkNotNull(bucket, "Bucket [%s] not found", bucketName);
    Blob blob = bucket.get(blobName);
    Preconditions.checkNotNull(blob, "blob [%s] not found", blobName);
    return blob.getContent();
  }

  private String lookupKey(String keyName) {
    String keyLocation = KEYS_DIR + keyName + KEY_EXTENSION;
    logger.debug("Getting app key for {} (blob {}) from bucket", keyName, keyLocation);
    byte[] rawKeyBytes = getRawBytes(keyLocation);
    checkState(rawKeyBytes != null, "Couldn't look up: " + keyName);
    String key = new String(rawKeyBytes).trim();
    return key;
  }

  private String lookupSecret(String secretName) throws IOException {
    String secretLocation = SECRETS_DIR + secretName + SECRET_EXTENSION;
    logger.debug("Getting app secret for {} (blob {})", secretName, secretLocation);
    byte[] encryptedSecret = getRawBytes(secretLocation);
    checkState(encryptedSecret != null, "Couldn't look up: " + secretName);
    String secret = new String(appSecretDecrypter.decryptAppSecret(encryptedSecret)).trim();
    checkState(!Strings.isNullOrEmpty(secret), "Couldn't decrypt: " + secretName);
    return secret;
  }
}
