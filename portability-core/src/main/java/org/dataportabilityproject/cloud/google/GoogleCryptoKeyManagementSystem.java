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
import java.io.IOException;
import org.dataportabilityproject.cloud.google.GoogleCloudFactory.CredentialsException;
import org.dataportabilityproject.cloud.interfaces.CryptoKeyManagementSystem;

final class GoogleCryptoKeyManagementSystem implements CryptoKeyManagementSystem {
  private CloudKMS cloudKMS;

  GoogleCryptoKeyManagementSystem() throws CredentialsException {
    HttpTransport transport = new NetHttpTransport();
    JsonFactory jsonFactory = new JacksonFactory();
    GoogleCredential credential;
    try {
      credential = GoogleCredential.getApplicationDefault(transport, jsonFactory);
    } catch (IOException e) {
      throw new CredentialsException(
          "Problem obtaining credentials via GoogleCredential.getApplicationDefault()");
    }
    if (credential.createScopedRequired()) {
      credential = credential.createScoped(CloudKMSScopes.all());
    }
    this.cloudKMS = new CloudKMS.Builder(transport, jsonFactory, credential)
        .setApplicationName("GoogleCryptoKeyManagementSystem")
        .build();
  }

  public byte[] decrypt(String cryptoKeyName, byte[] ciphertext) throws IOException {
    DecryptRequest request = new DecryptRequest().encodeCiphertext(ciphertext);
    DecryptResponse response = cloudKMS.projects().locations().keyRings().cryptoKeys()
        .decrypt(cryptoKeyName, request)
        .execute();

    return response.decodePlaintext();
  }
}
