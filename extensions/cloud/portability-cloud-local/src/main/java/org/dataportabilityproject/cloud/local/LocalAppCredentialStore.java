package org.dataportabilityproject.cloud.local;

import java.io.IOException;
import org.dataportabilityproject.spi.cloud.storage.AppCredentialStore;
import org.dataportabilityproject.types.transfer.auth.AppCredentials;

final class LocalAppCredentialStore implements AppCredentialStore {

  @Override
  public AppCredentials getAppCredentials(String keyName, String secretName) throws IOException {
    // TODO return from secrets.csv in jar. Use LocalSecrets from legacy code?
    return null;
  }
}
