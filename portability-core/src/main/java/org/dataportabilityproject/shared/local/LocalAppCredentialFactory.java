package org.dataportabilityproject.shared.local;

import static com.google.common.base.Preconditions.checkState;

import com.google.common.base.Strings;
import com.google.inject.Inject;
import org.dataportabilityproject.shared.AppCredentialFactory;
import org.dataportabilityproject.shared.AppCredentials;

public class LocalAppCredentialFactory implements AppCredentialFactory {
  private final LocalSecrets localSecrets;

  @Inject
  LocalAppCredentialFactory(LocalSecrets localSecrets) {
    this.localSecrets = localSecrets;
  }

  @Override
  public AppCredentials lookupAndCreate(String keyName, String secretName) {
    String key = localSecrets.get(keyName);
    checkState(!Strings.isNullOrEmpty(key), keyName + "is null");
    String secret = localSecrets.get(secretName);
    checkState(!Strings.isNullOrEmpty(secret), secretName + "is null");
    return AppCredentials.create(key, secret);
  }
}
