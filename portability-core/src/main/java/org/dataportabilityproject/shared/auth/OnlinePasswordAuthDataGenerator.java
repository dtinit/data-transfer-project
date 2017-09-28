package org.dataportabilityproject.shared.auth;

import com.google.common.base.Preconditions;
import java.io.IOException;
import org.dataportabilityproject.shared.Config;
import org.dataportabilityproject.shared.IOInterface;

public final class OnlinePasswordAuthDataGenerator implements OnlineAuthDataGenerator {

  @Override
  public AuthFlowInitiator generateAuthUrl(String id) throws IOException {
    return AuthFlowInitiator.create(Config.BASE_URL + "/simplelogin");
  }

  @Override
  public AuthData generateAuthData(String authCode, String id, AuthData initialAuthData, String extra)
      throws IOException {
    Preconditions.checkArgument(initialAuthData == null, "initial auth data not expected");
    return PasswordAuthData.create(authCode, extra);
  }
}
