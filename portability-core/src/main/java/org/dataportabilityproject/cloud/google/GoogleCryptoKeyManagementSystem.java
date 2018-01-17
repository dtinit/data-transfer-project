/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.dataportabilityproject.cloud.google;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.cloudkms.v1.CloudKMS;
import com.google.api.services.cloudkms.v1.CloudKMSScopes;
import com.google.api.services.cloudkms.v1.model.DecryptRequest;
import com.google.api.services.cloudkms.v1.model.DecryptResponse;
import com.google.inject.Inject;
import java.io.IOException;
import org.dataportabilityproject.cloud.google.GoogleCloudModule.ProjectId;
import org.dataportabilityproject.cloud.interfaces.CryptoKeyManagementSystem;

final class GoogleCryptoKeyManagementSystem implements CryptoKeyManagementSystem {
  // Key for encrypting app secrets.
  private static final String SECRETS_CRYPTO_KEY_FMT_STRING = "projects/%s/locations/global/"
      + "keyRings/portability_secrets/cryptoKeys/portability_secrets_key";

  private CloudKMS cloudKMS;
  private String secretsCryptoKey;

  @Inject
  GoogleCryptoKeyManagementSystem(@ProjectId String projectId) throws GoogleCredentialException {
    HttpTransport transport = new NetHttpTransport();
    JsonFactory jsonFactory = new JacksonFactory();
    GoogleCredential credential;
    try {
      credential = GoogleCredential.getApplicationDefault(transport, jsonFactory);
    } catch (IOException e) {
      throw new GoogleCredentialException(
          "Problem obtaining credentials via GoogleCredential.getApplicationDefault()");
    }
    if (credential.createScopedRequired()) {
      credential = credential.createScoped(CloudKMSScopes.all());
    }
    this.cloudKMS = new CloudKMS.Builder(transport, jsonFactory, credential)
        .setApplicationName("GoogleCryptoKeyManagementSystem")
        .build();
    this.secretsCryptoKey = String.format(SECRETS_CRYPTO_KEY_FMT_STRING, projectId);
  }

  public byte[] decryptAppSecret(byte[] ciphertext) throws IOException {
    DecryptRequest request = new DecryptRequest().encodeCiphertext(ciphertext);
    DecryptResponse response = cloudKMS.projects().locations().keyRings().cryptoKeys()
        .decrypt(secretsCryptoKey, request)
        .execute();

    return response.decodePlaintext();
  }
}
