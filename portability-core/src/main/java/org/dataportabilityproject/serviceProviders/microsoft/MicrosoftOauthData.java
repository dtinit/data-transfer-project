package org.dataportabilityproject.serviceProviders.microsoft;

import com.google.auto.value.AutoValue;
import org.dataportabilityproject.shared.auth.AuthData;

@AutoValue
abstract class MicrosoftOauthData extends AuthData {

  static MicrosoftOauthData create(String token, String accountAddress) {
    return new AutoValue_MicrosoftOauthData(token, accountAddress);
  }

  abstract String token();
  abstract String accountAddress();
}
