package org.dataportabilityproject.api.action.transfer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
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
import org.dataportabilityproject.types.client.transfer.PrepareImport;
import org.dataportabilityproject.types.client.transfer.Transfer;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.util.UUID;

import static org.dataportabilityproject.api.action.ActionUtils.decodeJobId;
import static org.dataportabilityproject.api.action.ActionUtils.encodeJobId;
import static org.dataportabilityproject.spi.api.auth.AuthServiceProviderRegistry.AuthMode.IMPORT;

/** Prepares a transfer request for the import service authentication flow */
public class PrepareImportAuthAction implements Action<PrepareImport, Transfer> {

  private final String baseApiUrl;
  private final JobStore jobStore;
  private final AuthServiceProviderRegistry registry;
  private final SymmetricKeyGenerator symmetricKeyGenerator;
  private final ObjectMapper objectMapper;

  @Inject
  PrepareImportAuthAction(
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
  public Class<PrepareImport> getRequestType() {
    return PrepareImport.class;
  }

  @SuppressWarnings("Duplicates")
  @Override
  public Transfer handle(PrepareImport prepareImport) {

    try {
      String id = prepareImport.getId();
      UUID jobId = decodeJobId(id);

      PortabilityJob job = jobStore.findJob(jobId);

      // Initial auth flow url
      AuthDataGenerator generator =
          registry.getAuthDataGenerator(job.importService(), job.transferDataType(), IMPORT);
      Preconditions.checkNotNull(
          generator,
          "Generator not found for type: %s, service: %s",
          job.transferDataType(),
          job.importService());

      String encodedJobId = encodeJobId(jobId);
      AuthFlowConfiguration configuration =
          generator.generateConfiguration(baseApiUrl, encodedJobId);

      // If present, store initial auth data for export services, e.g. used for oauth1
      if (configuration.getInitialAuthData() != null) {

        // Retrieve and parse the session key from the job
        String sessionKey = job.jobAuthorization().sessionSecretKey();
        SecretKey key = symmetricKeyGenerator.parse(BaseEncoding.base64Url().decode(sessionKey));

        // Ensure intial auth data for import has not already been set
        Preconditions.checkState(
            Strings.isNullOrEmpty(job.jobAuthorization().encryptedInitialImportAuthData()));

        // Serialize and encrypt the initial auth data
        String serialized = objectMapper.writeValueAsString(configuration.getInitialAuthData());
        String encryptedInitialAuthData = EncrypterFactory.create(key).encrypt(serialized);

        // Add the serialized and encrypted initial auth data to the job authorization
        JobAuthorization updatedJobAuthorization =
            job.jobAuthorization()
                .toBuilder()
                .setEncryptedInitialImportAuthData(encryptedInitialAuthData)
                .build();

        // Persist the updated PortabilityJob with the updated JobAuthorization
        PortabilityJob updatedJob =
            job.toBuilder().setAndValidateJobAuthorization(updatedJobAuthorization).build();

        jobStore.updateJob(jobId, updatedJob);
      }
      return new Transfer(
          id,
          Transfer.State.CREATED,
          configuration.getUrl(),
          job.exportService(),
          job.importService(),
          job.transferDataType());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
