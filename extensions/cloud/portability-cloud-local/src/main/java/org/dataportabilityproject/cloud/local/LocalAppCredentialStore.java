package org.dataportabilityproject.cloud.local;

import org.dataportabilityproject.spi.cloud.storage.AppCredentialStore;
import org.dataportabilityproject.types.transfer.auth.AppCredentials;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Implements an {@link AppCredentialStore} that sources secrets from a properties file visible from
 * this class' classloader.
 */
final class LocalAppCredentialStore implements AppCredentialStore {
  private static final String SECRETS_FILENAME = "secrets.properties";

  private Map<String, String> secrets = new HashMap<>();

  @SuppressWarnings("unchecked")
  public LocalAppCredentialStore() {
    // TODO Should there be a security check? For prod, we should read secrets from the cloud.
    try (InputStream stream = getClass().getClassLoader().getResourceAsStream(SECRETS_FILENAME)) {
      if (stream == null) {
        return;
      }

      Properties properties = new Properties();
      properties.load(stream);

      secrets.putAll((Map) properties);
    } catch (IOException e) {
      throw new IllegalStateException("Error loading local secrets", e);
    }
  }

  @Override
  public AppCredentials getAppCredentials(String keyName, String secretName) throws IOException {
    String keyValue = secrets.get(keyName);
    if (keyValue == null) {
      // TODO this should be a runtime exception
      throw new IOException("Key value not found for " + keyName);
    }
    String secretValue = secrets.get(secretName);
    if (secretValue == null) {
      // TODO this should be a runtime exception
      throw new IOException("Secret value not found for " + secretName);
    }

    return new AppCredentials(keyValue, secretValue);
  }
}
