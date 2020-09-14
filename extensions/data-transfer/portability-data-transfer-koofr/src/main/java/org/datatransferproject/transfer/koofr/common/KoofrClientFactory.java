package org.datatransferproject.transfer.koofr.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;

/** A factory for KoofrClient instances. */
public class KoofrClientFactory {
  private final String baseUrl;
  private final OkHttpClient client;
  private final OkHttpClient fileUploadClient;
  private final ObjectMapper objectMapper;
  private final Monitor monitor;
  private final KoofrCredentialFactory credentialFactory;

  public KoofrClientFactory(
      String baseUrl,
      OkHttpClient client,
      OkHttpClient fileUploadClient,
      ObjectMapper objectMapper,
      Monitor monitor,
      KoofrCredentialFactory credentialFactory) {
    this.baseUrl = baseUrl;
    this.client = client;
    this.fileUploadClient = fileUploadClient;
    this.objectMapper = objectMapper;
    this.monitor = monitor;
    this.credentialFactory = credentialFactory;
  }

  public KoofrClient create(TokensAndUrlAuthData authData) {
    KoofrClient koofrClient =
        new KoofrClient(
            baseUrl, client, fileUploadClient, objectMapper, monitor, credentialFactory);

    // Ensure credential is populated
    koofrClient.getOrCreateCredential(authData);

    return koofrClient;
  }
}
