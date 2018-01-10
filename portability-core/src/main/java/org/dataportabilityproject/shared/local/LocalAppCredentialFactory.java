/*
* Copyright 2017 The Data-Portability Project Authors.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* https://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
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
