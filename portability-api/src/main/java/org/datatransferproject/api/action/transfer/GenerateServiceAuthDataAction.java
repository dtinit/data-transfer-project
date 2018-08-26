package org.datatransferproject.api.action.transfer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.google.common.io.BaseEncoding;
import com.google.inject.Inject;
import org.datatransferproject.api.action.Action;
import org.datatransferproject.api.launcher.TypeManager;
import org.datatransferproject.security.DecrypterFactory;
import org.datatransferproject.security.SymmetricKeyGenerator;
import org.datatransferproject.spi.api.auth.AuthDataGenerator;
import org.datatransferproject.spi.api.auth.AuthServiceProviderRegistry;
import org.datatransferproject.spi.api.auth.AuthServiceProviderRegistry.AuthMode;
import org.datatransferproject.spi.cloud.storage.JobStore;
import org.datatransferproject.spi.cloud.types.PortabilityJob;
import org.datatransferproject.types.client.transfer.GenerateServiceAuthData;
import org.datatransferproject.types.client.transfer.ServiceAuthData;
import org.datatransferproject.types.transfer.auth.AuthData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.util.UUID;

import static org.datatransferproject.api.action.ActionUtils.decodeJobId;

/** Called after an import or export service authentication flow has successfully completed. */
public class GenerateServiceAuthDataAction
    implements Action<GenerateServiceAuthData, ServiceAuthData> {
  private static final Logger logger = LoggerFactory.getLogger(CreateTransferJobAction.class);

  private JobStore jobStore;
  private final AuthServiceProviderRegistry registry;
  private final SymmetricKeyGenerator symmetricKeyGenerator;
  private final ObjectMapper objectMapper;

  @Inject
  public GenerateServiceAuthDataAction(
          JobStore jobStore,
          AuthServiceProviderRegistry registry,
          SymmetricKeyGenerator symmetricKeyGenerator,
          TypeManager typeManager) {
    this.jobStore = jobStore;
    this.registry = registry;
    this.symmetricKeyGenerator = symmetricKeyGenerator;
    this.objectMapper = typeManager.getMapper();
  }

  @Override
  public Class<GenerateServiceAuthData> getRequestType() {
    return GenerateServiceAuthData.class;
  }

  public ServiceAuthData handle(GenerateServiceAuthData request) {
    try {
      String id = request.getId();
      Preconditions.checkNotNull(id, "transfer job ID required for GenerateServiceAuthDataAction");
      UUID jobId = decodeJobId(id);

      Preconditions.checkNotNull(request.getAuthToken(),
              "Auth token required for GenerateServiceAuthDataAction, transfer job ID: %s", jobId);
      PortabilityJob job = jobStore.findJob(jobId);
      Preconditions.checkNotNull(job, "existing job not found for transfer job ID: %s", jobId);


      // TODO: Determine service from job or from authUrl path?
      AuthMode authMode =
          GenerateServiceAuthData.Mode.EXPORT == request.getMode()
              ? AuthMode.EXPORT
              : AuthMode.IMPORT;
      String service = (authMode == AuthMode.EXPORT) ? job.exportService() : job.importService();

      // TODO(rtannenbaum): Remove logging, for testing only
      logger.info("Generating auth data for job id: {}, authMode: {}, service: {}, authToken: {}, callbackUrl: {}",
              jobId, authMode, service, request.getAuthToken(), request.getCallbackUrl());

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
          // TODO(rtannenbaum): Remove logging, for testing only
        logger.info("Had initial auth data in JobStore: {}", initialAuthData);
      }

      // TODO: Use UUID instead of UUID.toString()
      // Generate auth data
      AuthData authData =
          generator.generateAuthData(
                  request.getCallbackUrl(), request.getAuthToken(), jobId.toString(), initialAuthData, null);
      Preconditions.checkNotNull(authData, "Auth data should not be null");

      // Serialize and encrypt the auth data
      String serialized = objectMapper.writeValueAsString(authData);
      logger.info("Generated auth data for job: {}", jobId);
      return new ServiceAuthData(serialized);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
