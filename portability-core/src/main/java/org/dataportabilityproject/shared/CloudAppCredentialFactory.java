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
import com.google.inject.Inject;
import java.io.IOException;
import org.dataportabilityproject.cloud.interfaces.BucketStore;
import org.dataportabilityproject.cloud.interfaces.CloudFactory;
import org.dataportabilityproject.cloud.interfaces.CryptoKeyManagementSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CloudAppCredentialFactory implements AppCredentialFactory {
  static final Logger logger = LoggerFactory.getLogger(AppCredentials.class);

  private static final String BUCKET_NAME_PREFIX = "app-data-";
  private static final String KEYS_DIR = "keys/";
  private static final String KEY_EXTENSION = ".txt";
  private static final String SECRETS_DIR = "encrypted_secrets/";
  private static final String SECRET_EXTENSION = ".encrypted";
  private static final String CRYPTO_KEY_FMT_STRING = "projects/%s/locations/global/"
      + "keyRings/portability_secrets/cryptoKeys/portability_secrets_key";

  private final CloudFactory cloudFactory;

  @Inject
  CloudAppCredentialFactory(CloudFactory cloudFactory) {
    this.cloudFactory = cloudFactory;
  }

  @Override
  public AppCredentials lookupAndCreate(String keyName, String secretName) throws IOException {
    String projectId = cloudFactory.getProjectId();
    String bucketName = BUCKET_NAME_PREFIX + projectId;
    BucketStore bucketStore = cloudFactory.getBucketStore();
    CryptoKeyManagementSystem keyManagementSystem = cloudFactory.getCryptoKeyManagementSystem();

    String keyLocation = KEYS_DIR + keyName + KEY_EXTENSION;
    logger.debug("Getting key {} (blob {}) from bucket {}", keyName, keyLocation, bucketName);
    byte[] rawKeyBytes = bucketStore.getBlob(bucketName, keyLocation);
    String key = new String(rawKeyBytes);
    checkState(!Strings.isNullOrEmpty(key), "Couldn't lookup: " + keyName);
    String secretLocation = SECRETS_DIR + secretName + SECRET_EXTENSION;
    logger.debug("Getting secret {} (blob {}) from bucket {}", secretName, secretLocation,
        bucketName);
    byte[] encryptedSecret = bucketStore.getBlob(bucketName, secretLocation);
    String cryptoKeyName = String.format(CRYPTO_KEY_FMT_STRING, projectId);
    logger.debug("Decrypting secret with crypto key {}", cryptoKeyName);

    String secret =
        new String(keyManagementSystem.decrypt(cryptoKeyName, encryptedSecret));
    checkState(!Strings.isNullOrEmpty(key), "Couldn't lookup: " + secretName);
    return AppCredentials.create(key, secret);

  }
}
