package org.dataportabilityproject.serviceProviders.google;

import com.google.auto.value.AutoValue;
import org.dataportabilityproject.shared.auth.AuthData;

@AutoValue
abstract class GoogleTokenData extends AuthData {

  public static AuthData create(
      String accessToken,
      String refreshToken,
      String tokenServerEncodedUrl) {
    return new AutoValue_GoogleTokenData(accessToken, refreshToken, tokenServerEncodedUrl);
  }

  abstract String accessToken();
  abstract String refreshToken();
  abstract String tokenServerEncodedUrl();
}
