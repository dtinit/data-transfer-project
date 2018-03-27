package org.dataportabilityproject.cloud.microsoft.cosmos;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dataportabilityproject.api.launcher.ExtensionContext;
import org.dataportabilityproject.spi.cloud.extension.CloudExtension;
import org.dataportabilityproject.spi.cloud.storage.AppCredentialStore;
import org.dataportabilityproject.spi.cloud.storage.JobStore;

import java.util.Objects;

/**
 * Bootstraps the Microsoft cloud extension.
 *
 * <p>
 */
public class MicrosoftCloudExtension implements CloudExtension {
  private static final String VAULT_KEY = "microsoftVaultName";
  private static final String COSMOS_HOST = "microsoftCosmosHost";
  private static final String COSMOS_PORT = "microsoftCosmosPort";

  private static final String JOB_STORE_USER_NAME = "jobStoreUserName";
  private static final String JOB_STORE_PASSWORD = "jobStorePassword";

  private CosmosStore jobStore;
  private AzureKeyVaultStore vaultStore;

  @Override
  public void initialize(ExtensionContext context) {
    String vaultName = context.getSetting(VAULT_KEY, null);
    Objects.requireNonNull(vaultName, "Vault name not configured: " + VAULT_KEY);

    // create the vault
    vaultStore = new AzureKeyVaultStore(vaultName);

    // create the connection to Cosmos DB
    ObjectMapper mapper = context.getTypeManager().getMapper();

    String username = vaultStore.getSecret(JOB_STORE_USER_NAME);
    Objects.requireNonNull(username, "JobStore username not set in vault");

    String password = vaultStore.getSecret(JOB_STORE_PASSWORD);
    Objects.requireNonNull(password, "JobStore password not set in vault");

    String host = context.getSetting(COSMOS_HOST, null);
    Objects.requireNonNull(host, "Cosmos host not configured: " + COSMOS_HOST);

    int port = Integer.parseInt(context.getSetting(COSMOS_PORT, "10350"));

    jobStore = new CosmosStoreInitializer().createStore(username, password, host, port, mapper);
  }

  @Override
  public JobStore getJobStore() {
    return jobStore;
  }

  @Override
  public AppCredentialStore getAppCredentialStore() {
    return vaultStore;
  }
}
