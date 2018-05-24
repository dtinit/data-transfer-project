package org.dataportabilityproject.api.action.transfer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.BaseEncoding;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.dataportabilityproject.api.action.Action;
import org.dataportabilityproject.api.launcher.TypeManager;
import org.dataportabilityproject.security.EncrypterFactory;
import org.dataportabilityproject.security.SymmetricKeyGenerator;
import org.dataportabilityproject.spi.api.auth.AuthDataGenerator;
import org.dataportabilityproject.spi.api.auth.AuthServiceProviderRegistry;
import org.dataportabilityproject.spi.api.types.AuthFlowConfiguration;
import org.dataportabilityproject.spi.cloud.storage.JobStore;
import org.dataportabilityproject.spi.cloud.types.JobAuthorization;
import org.dataportabilityproject.spi.cloud.types.PortabilityJob;
import org.dataportabilityproject.types.client.transfer.CreateTransfer;
import org.dataportabilityproject.types.client.transfer.Transfer;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.util.UUID;

import static org.dataportabilityproject.api.action.ActionUtils.encodeJobId;

/** Creates a transfer request and prepares it for the export service authentication flow. */
public class CreateTransferAction implements Action<CreateTransfer, Transfer> {

  private final String baseApiUrl;
  private final JobStore jobStore;
  private final AuthServiceProviderRegistry registry;
  private final SymmetricKeyGenerator symmetricKeyGenerator;
  private final ObjectMapper objectMapper;

  @Inject
  CreateTransferAction(
      @Named("baseApiUrl") String baseApiUrl,
      JobStore jobStore,
      AuthServiceProviderRegistry registry,
      SymmetricKeyGenerator symmetricKeyGenerator,
      TypeManager typeManager) {
    this.baseApiUrl = baseApiUrl;
    this.jobStore = jobStore;
    this.registry = registry;
    this.symmetricKeyGenerator = symmetricKeyGenerator;
    this.objectMapper = typeManager.getMapper();
  }

  @Override
  public Class<CreateTransfer> getRequestType() {
    return CreateTransfer.class;
  }

  @Override
  public Transfer handle(CreateTransfer request) {
    String dataType = request.getTransferDataType();
    String exportService = request.getSource();
    String importService = request.getDestination();

    // Create a new job and persist
    UUID newId = UUID.randomUUID();
    SecretKey sessionKey = symmetricKeyGenerator.generate();
    String encodedSessionKey = BaseEncoding.base64Url().encode(sessionKey.getEncoded());

    String encodedJobId = encodeJobId(newId);

    PortabilityJob job = createJob(encodedSessionKey, dataType, exportService, importService);
    try {
      jobStore.createJob(newId, job);

      // Initial auth flow url
      AuthDataGenerator generator =
          registry.getAuthDataGenerator(
              request.getSource(),
              request.getTransferDataType(),
              AuthServiceProviderRegistry.AuthMode.EXPORT);

      AuthFlowConfiguration authFlowConfiguration =
          generator.generateConfiguration(baseApiUrl, newId.toString());

      // If present, store initial auth data for export services, e.g. used for oauth1
      if (authFlowConfiguration.getInitialAuthData() != null) {

        // Serialize and encrypt the initial auth data
        String serialized =
            objectMapper.writeValueAsString(authFlowConfiguration.getInitialAuthData());
        String encryptedInitialAuthData = EncrypterFactory.create(sessionKey).encrypt(serialized);

        // Add the serialized and encrypted initial auth data to the job authorization
        JobAuthorization updatedJobAuthorization =
            job.jobAuthorization()
                .toBuilder()
                .setEncryptedInitialExportAuthData(encryptedInitialAuthData)
                .build();

        // Persist the updated PortabilityJob with the updated JobAuthorization
        PortabilityJob updatedPortabilityJob =
            job.toBuilder().setAndValidateJobAuthorization(updatedJobAuthorization).build();

        jobStore.updateJob(newId, updatedPortabilityJob);
      }

      return new Transfer(
          encodedJobId,
          Transfer.State.CREATED,
          authFlowConfiguration.getUrl(),
          request.getSource(),
          request.getDestination(),
          request.getTransferDataType());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /** Populates the initial state of the {@link PortabilityJob} instance. */
  private static PortabilityJob createJob(
      String encodedSessionKey, String dataType, String exportService, String importService) {

    // Job auth data
    JobAuthorization jobAuthorization =
        JobAuthorization.builder()
            .setSessionSecretKey(encodedSessionKey)
            .setState(JobAuthorization.State.INITIAL)
            .build();

    return PortabilityJob.builder()
        .setTransferDataType(dataType)
        .setExportService(exportService)
        .setImportService(importService)
        .setAndValidateJobAuthorization(jobAuthorization)
        .build();
  }
}
