package org.dataportabilityproject.cloud.microsoft.cosmos;

import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azure.credentials.ApplicationTokenCredentials;
import com.microsoft.azure.keyvault.KeyVaultClient;
import com.microsoft.azure.keyvault.models.SecretBundle;
import org.dataportabilityproject.spi.cloud.storage.AppCredentialStore;
import org.dataportabilityproject.types.transfer.auth.AppCredentials;

import java.io.IOException;
import java.util.Objects;

import static java.lang.System.getenv;

/** A secrets store backed by Azure Key Vault. */
public class AzureKeyVaultStore implements AppCredentialStore {
  private static final String VAULT_ADDRESS = "https://%s.vault.azure.net/";

  private final String vaultUrl;
  private final KeyVaultClient vaultClient;

  public AzureKeyVaultStore(String keyVaultName) {
    vaultUrl = String.format(VAULT_ADDRESS, keyVaultName);

    // NB: env vars should be sourced in a secure form
    // TODO should env vars be sourced from config?
    String azureClientId = getenv("AZURE_CLIENT_ID");
    Objects.requireNonNull(azureClientId, "AZURE_CLIENT_ID not set in environment");

    String azureTenantId = getenv("AZURE_TENANT_ID");
    Objects.requireNonNull(azureTenantId, "AZURE_TENANT_ID not set in environment");

    String azureClientSecret = getenv("AZURE_CLIENT_SECRET");
    Objects.requireNonNull(azureClientSecret, "AZURE_CLIENT_SECRET not set in environment");

    ApplicationTokenCredentials credentials =
        new ApplicationTokenCredentials(
            azureClientId, azureTenantId, azureClientSecret, AzureEnvironment.AZURE);
    vaultClient = new KeyVaultClient(credentials);
  }

  @Override
  public AppCredentials getAppCredentials(String keyName, String secretName) throws IOException {
    SecretBundle secretBundle = vaultClient.getSecret(vaultUrl, "/secrets/" + keyName);
    if (secretBundle == null) {
      throw new IOException(("Secret not found: " + secretName));
    }
    return new AppCredentials(keyName, secretBundle.value());
  }

  public String getSecret(String key) {
    SecretBundle secretBundle = vaultClient.getSecret(vaultUrl, "/secrets/" + key);
    return secretBundle == null ? null : secretBundle.value();
  }
}
