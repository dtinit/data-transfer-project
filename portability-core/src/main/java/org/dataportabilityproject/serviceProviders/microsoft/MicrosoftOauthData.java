package org.dataportabilityproject.serviceProviders.microsoft;

import com.google.auto.value.AutoValue;
import org.dataportabilityproject.shared.auth.AuthData;

@AutoValue
abstract class MicrosoftOauthData extends AuthData {

  static MicrosoftOauthData create(String accessToken,
      String refreshToken,
      String tokenServerEncodedUrl,
      String accountAddress) {
    return new AutoValue_MicrosoftOauthData(accessToken, refreshToken, tokenServerEncodedUrl, accountAddress);
  }

  abstract String accessToken();
  abstract String refreshToken();
  abstract String tokenServerEncodedUrl();
  abstract String accountAddress(); // TODO: remove if not needed
}
