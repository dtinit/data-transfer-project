package org.datatransferproject.api.action.transfer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.google.common.io.BaseEncoding;
import com.google.inject.Inject;
import org.datatransferproject.api.action.Action;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.api.launcher.TypeManager;
import org.datatransferproject.launcher.monitor.events.EventCode;
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

import javax.crypto.SecretKey;
import java.io.IOException;
import java.util.UUID;

import static java.lang.String.format;
import static org.datatransferproject.api.action.ActionUtils.decodeJobId;

/** Called after an import or export service authentication flow has successfully completed. */
public class GenerateServiceAuthDataAction
    implements Action<GenerateServiceAuthData, ServiceAuthData> {
  private JobStore jobStore;
  private final AuthServiceProviderRegistry registry;
  private final SymmetricKeyGenerator symmetricKeyGenerator;
  private final ObjectMapper objectMapper;
  private DecrypterFactory decrypterFactory;
  private final Monitor monitor;

  @Inject
  public GenerateServiceAuthDataAction(
      JobStore jobStore,
      AuthServiceProviderRegistry registry,
      SymmetricKeyGenerator symmetricKeyGenerator,
      TypeManager typeManager,
      Monitor monitor) {
    this.jobStore = jobStore;
    this.registry = registry;
    this.symmetricKeyGenerator = symmetricKeyGenerator;
    this.objectMapper = typeManager.getMapper();
    this.decrypterFactory = new DecrypterFactory(monitor);
    this.monitor = monitor;
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

      Preconditions.checkNotNull(
          request.getAuthToken(),
          "Auth token required for GenerateServiceAuthDataAction, transfer job ID: %s",
          jobId);
      PortabilityJob job = jobStore.findJob(jobId);
      Preconditions.checkNotNull(job, "existing job not found for transfer job ID: %s", jobId);

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
        String serialized = decrypterFactory.create(key).decrypt(encryptedInitialAuthData);
        initialAuthData = objectMapper.readValue(serialized, AuthData.class);
      }

      // TODO: Use UUID instead of UUID.toString()
      // Generate auth data
      AuthData authData =
          generator.generateAuthData(
              request.getCallbackUrl(),
              request.getAuthToken(),
              jobId.toString(),
              initialAuthData,
              null);
      Preconditions.checkNotNull(authData, "Auth data should not be null");

      monitor.debug(
          () -> format("Generated auth data in mode '%s' for job: %s", authMode, jobId), jobId,
          EventCode.API_GENERATED_AUTH_DATA);

      // Serialize the auth data
      String serialized = objectMapper.writeValueAsString(authData);
      return new ServiceAuthData(serialized);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
