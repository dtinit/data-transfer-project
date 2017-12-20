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
package org.dataportabilityproject.shared;

import com.google.auto.value.AutoValue;
import com.google.gdata.util.common.base.Pair;
import java.io.IOException;
import org.dataportabilityproject.PortabilityFlags;
import org.dataportabilityproject.cloud.CloudFactoryFactory;
import org.dataportabilityproject.cloud.SupportedCloud;
import org.dataportabilityproject.cloud.interfaces.BucketStore;
import org.dataportabilityproject.cloud.interfaces.CloudFactory;
import org.dataportabilityproject.cloud.interfaces.CryptoKeyManagementSystem;
import org.dataportabilityproject.shared.local.LocalSecrets;

/**
 * Holder for an application key and secret.
 */
@AutoValue
public abstract class AppCredentials {
    private static final String BUCKET_NAME_PREFIX = "app-data-";
    private static final String KEYS_DIR = "keys/";
    private static final String KEY_EXTENSION = ".txt";
    private static final String SECRETS_DIR = "encrypted_secrets/";
    private static final String SECRET_EXTENSION = ".encrypted";
    private static final String CRYPTO_KEY_FMT_STRING = "projects/%s/locations/global/"
        + "keyRings/portability_secrets/cryptoKeys/portability_secrets_key";

    public abstract String key();
    public abstract String secret();

    public static AppCredentials create(String key, String secret) {
        return new AutoValue_AppCredentials(key, secret);
    }

    public static AppCredentials lookupAndCreate(String keyName, String secretName) {
        Pair<String, String> keyAndSecret;
        try {
            keyAndSecret = lookupKeyAndSecret(keyName, secretName);
        } catch (IOException e) {
            throw new IllegalStateException(
                String.format("Problem looking up key or secret for %s or %s. %s",
                    keyName, secretName, e));
        }
        try {
            return AppCredentials.create(keyAndSecret.first, keyAndSecret.second);
        } catch (NullPointerException e) {
            throw new IllegalStateException(
                String.format("Missing key or secret for %s or %s. %s", keyName, secretName, e));
        }
    }

    private static Pair<String, String> lookupKeyAndSecret(String keyName, String secretName)
        throws IOException {
        if (PortabilityFlags.cloud() == SupportedCloud.LOCAL) {
            String key = LocalSecrets.getInstance().get(keyName);
            String secret = LocalSecrets.getInstance().get(secretName);
            return new Pair<>(key, secret);
        }
        CloudFactory cloudFactory = CloudFactoryFactory.getCloudFactory(PortabilityFlags.cloud());
        String projectId = cloudFactory.getProjectId();
        String bucketName = BUCKET_NAME_PREFIX + projectId;
        BucketStore bucketStore = cloudFactory.getBucketStore();
        CryptoKeyManagementSystem keyManagementSystem = cloudFactory.getCryptoKeyManagementSystem();

        String keyLocation = KEYS_DIR + keyName + KEY_EXTENSION;
        LogUtils.log("Getting key %s (blob %s) from bucket %s", keyName, keyLocation, bucketName);
        byte[] rawKeyBytes = bucketStore.getBlob(bucketName, keyLocation);
        String key = new String(rawKeyBytes);
        String secretLocation = SECRETS_DIR + secretName + SECRET_EXTENSION;
        LogUtils.log("Getting secret %s (blob %s) from bucket %s", secretName, secretLocation,
            bucketName);
        byte[] encryptedSecret = bucketStore.getBlob(bucketName, secretLocation);
        String cryptoKeyName = String.format(CRYPTO_KEY_FMT_STRING, projectId);
        LogUtils.log("Decrypting secret with crypto key %s", cryptoKeyName);
        String secret =
            new String(keyManagementSystem.decrypt(cryptoKeyName, encryptedSecret));
        return new Pair<>(key, secret);
    }
}
