package org.dataportabilityproject.shared.auth;

import java.io.IOException;
import org.dataportabilityproject.shared.IOInterface;

public final class OfflinePasswordAuthDataGenerator implements OfflineAuthDataGenerator {

  @Override
  public AuthData generateAuthData(IOInterface ioInterface) throws IOException {
    String account = ioInterface.ask("Enter email account");
    String password = ioInterface.ask("Enter email account password");

    return PasswordAuthData.create(account, password);
  }
}
