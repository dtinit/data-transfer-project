package org.dataportabilityproject.shared;

import java.io.IOException;

public interface AppCredentialFactory {
  AppCredentials lookupAndCreate(String keyName, String secretName) throws IOException;
}
