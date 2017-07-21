package org.dataportabilityproject.shared.auth;

import com.google.common.base.Preconditions;
import java.io.IOException;
import org.dataportabilityproject.shared.IOInterface;

public final class OnlinePasswordAuthDataGenerator implements OnlineAuthDataGenerator {
  private static final String BASE_URL = "http://localhost:3000";

  @Override
  public AuthFlowInitiator generateAuthUrl(String id) throws IOException {
    return AuthFlowInitiator.create(BASE_URL + "/simplelogin");
  }

  @Override
  public AuthData generateAuthData(String authCode, String id, AuthData initialAuthData, String extra)
      throws IOException {
    Preconditions.checkArgument(initialAuthData == null, "initial auth data not expected");
    return PasswordAuthData.create(authCode, extra);
  }
}
