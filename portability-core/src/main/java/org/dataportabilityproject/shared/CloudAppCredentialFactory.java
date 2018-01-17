/*
* Copyright 2017 The Data-Portability Project Authors.
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
package org.dataportabilityproject.shared;

import static com.google.common.base.Preconditions.checkState;

import com.google.common.base.Strings;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.dataportabilityproject.cloud.interfaces.BucketStore;
import org.dataportabilityproject.cloud.interfaces.CloudFactory;
import org.dataportabilityproject.cloud.interfaces.CryptoKeyManagementSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class CloudAppCredentialFactory implements AppCredentialFactory {
  static final Logger logger = LoggerFactory.getLogger(AppCredentials.class);

  /**
   * Store keys and secrets in a cache so we can reduce load on cloud storage / KMS when
   * reading keys and reading/decrypting secrets several times on startup.
   *
   * <p>Set the cache to reload keys/secrets periodically so that in the event of a key/secret being
   * compromised, we can update them without restarting our servers.
   */
  private LoadingCache<String, String> keys;
  private LoadingCache<String, String> secrets;

  private static final String KEYS_DIR = "keys/";
  private static final String KEY_EXTENSION = ".txt";
  private static final String SECRETS_DIR = "encrypted_secrets/";
  private static final String SECRET_EXTENSION = ".encrypted";

  private final BucketStore bucketStore;
  private final CryptoKeyManagementSystem cryptoKeyManagementSystem;

  @Inject
  CloudAppCredentialFactory(CloudFactory cloudFactory) {
    this.bucketStore = cloudFactory.getBucketStore();
    this.cryptoKeyManagementSystem = cloudFactory.getCryptoKeyManagementSystem();
    this.keys = CacheBuilder.newBuilder().expireAfterWrite(10, TimeUnit.MINUTES).build(
        new CacheLoader<String, String>() {
          @Override
          public String load(String key) throws Exception {
            return lookupKey(key);
          }
        });
    this.secrets = CacheBuilder.newBuilder().expireAfterWrite(10, TimeUnit.MINUTES).build(
        new CacheLoader<String, String>() {
          @Override
          public String load(String key) throws Exception {
            return lookupSecret(key);
          }
        });
  }

  @Override
  public AppCredentials lookupAndCreate(String keyName, String secretName) throws IOException {
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

    return AppCredentials.create(key, secret);
  }

  private String lookupKey(String keyName) {
    String keyLocation = KEYS_DIR + keyName + KEY_EXTENSION;
    logger.debug("Getting app key for {} (blob {}) from bucket", keyName, keyLocation);
    byte[] rawKeyBytes = bucketStore.getAppCredentialBlob(keyLocation);
    checkState(rawKeyBytes != null, "Couldn't look up: " + keyName);
    String key = new String(rawKeyBytes);
    return key;
  }

  private String lookupSecret(String secretName) throws IOException {
    String secretLocation = SECRETS_DIR + secretName + SECRET_EXTENSION;
    logger.debug("Getting app secret for {} (blob {})", secretName, secretLocation);
    byte[] encryptedSecret = bucketStore.getAppCredentialBlob(secretLocation);
    checkState(encryptedSecret != null, "Couldn't look up: " + secretName);
    String secret = new String(cryptoKeyManagementSystem.decryptAppSecret(encryptedSecret));
    checkState(!Strings.isNullOrEmpty(secret), "Couldn't decrypt: " + secretName);
    return secret;
  }
}
