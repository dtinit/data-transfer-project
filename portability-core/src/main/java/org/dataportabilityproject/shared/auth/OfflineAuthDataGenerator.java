package org.dataportabilityproject.shared.auth;


import java.io.IOException;
import org.dataportabilityproject.shared.IOInterface;

public interface OfflineAuthDataGenerator {
  AuthData generateAuthData(IOInterface ioInterface) throws IOException;
}
