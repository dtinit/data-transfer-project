package org.dataportabilityproject.spi.cloud.auth;

import java.io.IOException;
import org.dataportabilityproject.types.transfer.auth.AppCredentials;

public interface AppCredentialFactory {
  AppCredentials get(String keyName, String secretName) throws IOException;
}
