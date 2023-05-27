package org.datatransferproject.cloud.local;

import org.datatransferproject.spi.cloud.storage.AppCredentialStore;
import org.datatransferproject.types.transfer.auth.AppCredentials;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Implements an {@link AppCredentialStore} that sources secrets from environment variables.
 */
final class LocalAppCredentialStore implements AppCredentialStore {

    // Why use environment variables and not docker secrets? Because developers run
    // DTP locally both inside of Docker and as a raw JAR.  By just relying on environment
    // variables it makes it easier to run DTP from the Jar locally.
    public LocalAppCredentialStore() {
        // TODO Should there be a security check? For prod, we should read secrets from the cloud.
    }

    @Override
    public AppCredentials getAppCredentials(String keyName, String secretName) throws IOException {
        String keyValue = System.getenv(keyName);
        if (keyValue == null) {
            // TODO this should be a runtime exception
            throw new IOException("Key value " + keyName + " not set as environment variable");
        }
        String secretValue = System.getenv(secretName);
        if (secretValue == null) {
            // TODO this should be a runtime exception
            throw new IOException("Key value " + secretName + " not set as environment variable");
        }
        return new AppCredentials(keyValue, secretValue);
    }
}
