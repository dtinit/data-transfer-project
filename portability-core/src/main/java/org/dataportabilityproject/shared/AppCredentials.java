package org.dataportabilityproject.shared;

import com.google.auto.value.AutoValue;

/**
 * Holder for an application key and secret.
 */
@AutoValue
public abstract class AppCredentials {
    public abstract String key();
    public abstract String secret();

    public static AppCredentials create(String key, String secret) {
        return new AutoValue_AppCredentials(key, secret);
    }

    public static AppCredentials create(Secrets secrets, String keyName, String secretName) {
        try {
            return create(secrets.get(keyName), secrets.get(secretName));
        } catch (NullPointerException e) {
            throw new IllegalStateException(
                String.format("Missing or incorrect values for %s or %s in secrets.csv",
                    keyName, secretName));
        }
    }
}
