package org.dataportabilityproject.api.action.transfer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.google.common.io.BaseEncoding;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.dataportabilityproject.api.action.Action;
import org.dataportabilityproject.api.launcher.TypeManager;
import org.dataportabilityproject.security.DecrypterFactory;
import org.dataportabilityproject.security.EncrypterFactory;
import org.dataportabilityproject.security.SymmetricKeyGenerator;
import org.dataportabilityproject.spi.api.auth.AuthDataGenerator;
import org.dataportabilityproject.spi.api.auth.AuthServiceProviderRegistry;
import org.dataportabilityproject.spi.api.auth.AuthServiceProviderRegistry.AuthMode;
import org.dataportabilityproject.spi.cloud.storage.JobStore;
import org.dataportabilityproject.spi.cloud.types.PortabilityJob;
import org.dataportabilityproject.types.client.transfer.GenerateServiceAuthData;
import org.dataportabilityproject.types.client.transfer.ServiceAuthData;
import org.dataportabilityproject.types.transfer.auth.AuthData;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.util.UUID;

import static org.dataportabilityproject.api.action.ActionUtils.decodeJobId;

/** Called after an import or export service authentication flow has successfully completed. */
public class GenerateServiceAuthDataAction
    implements Action<GenerateServiceAuthData, ServiceAuthData> {
  private JobStore jobStore;
  private final AuthServiceProviderRegistry registry;
  private final SymmetricKeyGenerator symmetricKeyGenerator;
  private final ObjectMapper objectMapper;
  private final String baseApiUrl;

  @Inject
  public GenerateServiceAuthDataAction(
      JobStore jobStore,
      AuthServiceProviderRegistry registry,
      SymmetricKeyGenerator symmetricKeyGenerator,
      TypeManager typeManager,
      @Named("baseApiUrl") String baseApiUrl) {
    this.jobStore = jobStore;
    this.registry = registry;
    this.symmetricKeyGenerator = symmetricKeyGenerator;
    this.baseApiUrl = baseApiUrl;
    this.objectMapper = typeManager.getMapper();
  }

  @Override
  public Class<GenerateServiceAuthData> getRequestType() {
    return GenerateServiceAuthData.class;
  }

  public ServiceAuthData handle(GenerateServiceAuthData request) {
    try {
      UUID jobId = decodeJobId(request.getId());

      PortabilityJob job = jobStore.findJob(jobId);
      Preconditions.checkNotNull(job, "existing job not found for jobId: %s", jobId);

      // TODO: Determine service from job or from authUrl path?
      AuthMode authMode =
          GenerateServiceAuthData.Mode.EXPORT == request.getMode()
              ? AuthMode.EXPORT
              : AuthMode.IMPORT;
      String service = (authMode == AuthMode.EXPORT) ? job.exportService() : job.importService();

      AuthDataGenerator generator =
          registry.getAuthDataGenerator(service, job.transferDataType(), authMode);

      // Obtain the session key for this job
      String encodedSessionKey = job.jobAuthorization().sessionSecretKey();
      SecretKey key =
          symmetricKeyGenerator.parse(BaseEncoding.base64Url().decode(encodedSessionKey));

      // Retrieve initial auth data, if it existed
      AuthData initialAuthData = null;
      String encryptedInitialAuthData =
          (authMode == AuthMode.EXPORT)
              ? job.jobAuthorization().encryptedInitialExportAuthData()
              : job.jobAuthorization().encryptedInitialImportAuthData();
      if (encryptedInitialAuthData != null) {
        // Retrieve and parse the session key from the job
        // Decrypt and deserialize the object
        String serialized = DecrypterFactory.create(key).decrypt(encryptedInitialAuthData);
        initialAuthData = objectMapper.readValue(serialized, AuthData.class);
      }

      // TODO: Use UUID instead of UUID.toString()
      // Generate auth data
      AuthData authData =
          generator.generateAuthData(
              baseApiUrl, request.getAuthToken(), jobId.toString(), initialAuthData, null);
      Preconditions.checkNotNull(authData, "Auth data should not be null");

      // Serialize and encrypt the auth data
      String serialized = objectMapper.writeValueAsString(authData);
      String encryptedAuthData = EncrypterFactory.create(key).encrypt(serialized);
      return new ServiceAuthData(encryptedAuthData);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
