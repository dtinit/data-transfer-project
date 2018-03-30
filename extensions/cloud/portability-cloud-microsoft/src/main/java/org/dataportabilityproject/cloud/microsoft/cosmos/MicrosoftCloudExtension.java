package org.dataportabilityproject.cloud.microsoft.cosmos;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dataportabilityproject.api.launcher.ExtensionContext;
import org.dataportabilityproject.spi.cloud.extension.CloudExtension;
import org.dataportabilityproject.spi.cloud.storage.AppCredentialStore;
import org.dataportabilityproject.spi.cloud.storage.JobStore;

import java.util.Objects;

import static java.lang.System.getenv;

/**
 * Bootstraps the Microsoft cloud extension. The extension uses Azure Key Vault for storing secrets
 * and Azure Cosmos DB and Blob Storage for persistence.
 *
 * <p>During initialization, this extension first establishes a connection to the Azure Key Vault to
 * obtain the account key secret for accessing Cosmos DB. As the configuration for connecting to the
 * vault is sensitive, it is expect that the runtime process orchestrator will provide it via
 * environment variables in a secure manner.
 *
 * <p>The following configuration values (environment or runtime configuration) are required:
 *
 * <ul>
 *   <li>{@link #AZURE_VAULT_NAME}: The name of the Azure Vault instance
 *   <li>{@link #AZURE_VAULT_TENANT_ID}: The name of the Active Directory tenant
 *   <li>{@link #AZURE_VAULT_CLIENT_ID}: The id of the data portability app registered in Azure
 *       Active Directory
 *   <li>{@link #AZURE_VAULT_CLIENT_SECRET}: A secret set in Active Directory as the data
 *       portability app password
 *   <li>{@link #AZURE_COSMOS_ACCOUNT_NAME}: The account name of the Azure Cosmos DB instance.
 *   <li>{@link #AZURE_COSMOS_PARTITION_KEY}: Optional. Used for selecting a Cosmos DB partition if
 *       desired.
 * </ul>
 *
 * Azure Setup.
 *
 * <p>The following services must be setup on Azure:
 *
 * <ul>
 *   <li>Active Directory: Active Directory must be enabled with a tenant
 *   <li>Data Portability App: The data portability app must be registered as an application with
 *       Active Directory and have a secret key set. The application id and secret key will be used
 *       to access Azure Key Vault
 *   <li>Cosmos DB: A Cosmos DB instance must be enabled and a secret account key created.
 *   <li>Microsoft Graph API must be enabled to allow access to the data portability app and OAuth
 *       callback URLs set
 *   <li>Key Vault: Azure Key Vault must be enabled. Access permissions must be granted to the data
 *       portability app registered with Active Directory.
 *   <li>Key Vault: A key {@link #AZURE_COSMOS_ACCOUNT_KEY} must be added, which is the secret used
 *       to access Cosmos DB.
 *   <li>Key Vault: A key {@link #AZURE_BLOB_KEY} must be added, which is the secret used
 *       to access Blob Storage.
 *   <li>Key Vault: Two secrets must be added for each data transfer extension, in the form
 *       [EXTENSION]KEY and [EXTENSION]SECRET.
 *   <li>Key Vault: JWT secrets JWTKEY and JWTSECRET must be added.
 */
public class MicrosoftCloudExtension implements CloudExtension {
  // Config for accessing the Azure Vault (sensitive)
  static final String AZURE_VAULT_NAME = "AZURE_VAULT_NAME_KEY";

  // Derived from the Active Directory tenant id
  static final String AZURE_VAULT_TENANT_ID = "AZURE_VAULT_TENANT_ID";

  // Derived from the Active Directory application registration id
  static final String AZURE_VAULT_CLIENT_ID = "AZURE_VAULT_CLIENT_ID";

  // Derived from the Active Directory application registration secret key
  static final String AZURE_VAULT_CLIENT_SECRET = "AZURE_VAULT_CLIENT_SECRET";

  // Non-sensitive configuration to access Cosmos DB
  static final String AZURE_COSMOS_ACCOUNT_NAME = "AZURE_COSMOS_ACCOUNT_NAME";
  static final String AZURE_COSMOS_PARTITION_KEY = "AZURE_COSMOS_PARTITION_KEY";

  // The key to access Cosmos DB stored in Azure Vault (sensitive)
  static final String AZURE_COSMOS_ACCOUNT_KEY = "TABLESTOREACCOUNTKEY";

  // The key to access Blob Storage stored in Azure Vault (sensitive)
  static final String AZURE_BLOB_KEY = "AZUREBLOBKEY";

  private AzureTableStore jobStore;
  private AzureKeyVaultStore vaultStore;

  @Override
  public void initialize(ExtensionContext context) {
    // load the configuration values to access the vault

    // TODO should env vars be sourced from config?
    String vaultName = loadSecretValue(AZURE_VAULT_NAME, true);
    String tenantId = loadSecretValue(AZURE_VAULT_TENANT_ID, true);
    String clientId = loadSecretValue(AZURE_VAULT_CLIENT_ID, true);
    String clientSecret = loadSecretValue(AZURE_VAULT_CLIENT_SECRET, true);

    // create the vault
    vaultStore = new AzureKeyVaultStore(vaultName, tenantId, clientId, clientSecret);

    // load the configuration to access Cosmos DB
    TableStoreConfiguration.Builder builder = TableStoreConfiguration.Builder.newInstance();

    String accountKey = getVaultKey(AZURE_COSMOS_ACCOUNT_KEY);
    builder.accountKey(accountKey);

    String blobKey = getVaultKey(AZURE_BLOB_KEY);
    builder.blobKey(blobKey);

    String accountName = loadSecretValue(AZURE_COSMOS_ACCOUNT_NAME, true);
    builder.accountName(accountName);

    String partitionKey = loadSecretValue(AZURE_COSMOS_PARTITION_KEY, false);
    if (partitionKey == null) {
      partitionKey = "DefaultPartition";
    }
    builder.partitionKey(partitionKey);

    ObjectMapper mapper = context.getTypeManager().getMapper();
    builder.mapper(mapper);

    // create the connection to the database
    jobStore = new AzureTableStore(builder.build());
    jobStore.init();
  }

  @Override
  public JobStore getJobStore() {
    return jobStore;
  }

  @Override
  public AppCredentialStore getAppCredentialStore() {
    return vaultStore;
  }

  private String getVaultKey(String key) {
    String secret = vaultStore.getSecret(key);
    if (secret == null) {
      throw new RuntimeException("Unable to retrieve secret from Azure Vault: " + key);
    }
    return secret;
  }

  private String loadSecretValue(String key, boolean required) {
    String tenantId = getenv(key);
    if (tenantId == null) {
      tenantId = System.getProperty(key);
    }
    if (required) {
      Objects.requireNonNull(tenantId, key + " not set in environment or configuration");
    }
    return tenantId;
  }
}
