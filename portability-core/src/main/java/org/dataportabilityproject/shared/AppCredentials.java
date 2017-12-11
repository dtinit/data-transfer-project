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
    private static final String BUCKET_NAME = "app-data-portability";
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
        BucketStore bucketStore = cloudFactory.getBucketStore();
        CryptoKeyManagementSystem keyManagementSystem = cloudFactory.getCryptoKeyManagementSystem();

        String keyLocation = KEYS_DIR + keyName + KEY_EXTENSION;
        LogUtils.log("Getting key %s (blob %s) from bucket %s", keyName, keyLocation,
            BUCKET_NAME);
        byte[] rawKeyBytes = bucketStore.getBlob(BUCKET_NAME, keyLocation);
        String key = new String(rawKeyBytes);
        String secretLocation = SECRETS_DIR + secretName + SECRET_EXTENSION;
        LogUtils.log("Getting secret %s (blob %s) from bucket %s", secretName, secretLocation,
            BUCKET_NAME);
        byte[] encryptedSecret = bucketStore.getBlob(BUCKET_NAME, secretLocation);
        String cryptoKeyName = String.format(CRYPTO_KEY_FMT_STRING,
            System.getenv("GOOGLE_PROJECT_ID").toLowerCase());
        LogUtils.log("Decrypting secret with crypto key %s", cryptoKeyName);
        String secret =
            new String(keyManagementSystem.decrypt(cryptoKeyName, encryptedSecret));
        return new Pair<>(key, secret);
    }
}
