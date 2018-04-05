package org.dataportabilityproject.cloud.microsoft.cosmos;

import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azure.credentials.ApplicationTokenCredentials;
import com.microsoft.azure.keyvault.KeyVaultClient;
import com.microsoft.azure.keyvault.models.SecretBundle;
import org.dataportabilityproject.spi.cloud.storage.AppCredentialStore;
import org.dataportabilityproject.types.transfer.auth.AppCredentials;

import java.io.IOException;

/** A secrets store backed by Azure Key Vault. */
public class AzureKeyVaultStore implements AppCredentialStore {
  private static final String VAULT_ADDRESS = "https://%s.vault.azure.net/";

  private final String vaultUrl;
  private final KeyVaultClient vaultClient;

  public AzureKeyVaultStore(
      String keyVaultName, String tenantId, String clientId, String clientSecret) {
    vaultUrl = String.format(VAULT_ADDRESS, keyVaultName);

    ApplicationTokenCredentials credentials =
        new ApplicationTokenCredentials(clientId, tenantId, clientSecret, AzureEnvironment.AZURE);
    vaultClient = new KeyVaultClient(credentials);
  }

  @Override
  public AppCredentials getAppCredentials(String keyName, String secretName) throws IOException {
    String normalizedKey = normalize(keyName);
    SecretBundle keyBundle = vaultClient.getSecret(vaultUrl, normalizedKey);
    if (keyBundle == null) {
      throw new IOException(("Key not found: " + secretName));
    }
    String keyValue = keyBundle.value();

    String normalizedSecret = normalize(secretName);
    SecretBundle secretBundle = vaultClient.getSecret(vaultUrl, normalizedSecret);
    if (secretBundle == null) {
      throw new IOException(("Key not found: " + secretName));
    }
    String secretValue = secretBundle.value();

    return new AppCredentials(keyValue, secretValue);
  }

  public String getSecret(String key) {
    String normalizedKey = normalize(key);
    SecretBundle secretBundle = vaultClient.getSecret(vaultUrl, normalizedKey);
    return secretBundle == null ? null : secretBundle.value();
  }

  /** Normalizes a key to adhere to Azure Vault naming requirements. */
  private static String normalize(String key) {
    return key.replace("_", "");
  }
}
