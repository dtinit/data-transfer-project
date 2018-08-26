package org.datatransferproject.api.action.transfer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.io.BaseEncoding;
import com.google.inject.Inject;
import org.datatransferproject.api.action.Action;
import org.datatransferproject.api.launcher.TypeManager;
import org.datatransferproject.security.EncrypterFactory;
import org.datatransferproject.security.SymmetricKeyGenerator;
import org.datatransferproject.spi.api.auth.AuthDataGenerator;
import org.datatransferproject.spi.api.auth.AuthServiceProviderRegistry;
import org.datatransferproject.spi.api.types.AuthFlowConfiguration;
import org.datatransferproject.spi.cloud.storage.JobStore;
import org.datatransferproject.spi.cloud.types.JobAuthorization;
import org.datatransferproject.spi.cloud.types.PortabilityJob;
import org.datatransferproject.types.client.transfer.CreateTransferJob;
import org.datatransferproject.types.client.transfer.TransferJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.util.UUID;

import static org.datatransferproject.api.action.ActionUtils.encodeJobId;
import static org.datatransferproject.spi.api.auth.AuthServiceProviderRegistry.AuthMode.EXPORT;
import static org.datatransferproject.spi.api.auth.AuthServiceProviderRegistry.AuthMode.IMPORT;

/**
 * Creates a transfer job and prepares it for both the export and import service authentication
 * flow.
 */
public class CreateTransferJobAction implements Action<CreateTransferJob, TransferJob> {
  private static final Logger logger = LoggerFactory.getLogger(CreateTransferJobAction.class);

  private final JobStore jobStore;
  private final AuthServiceProviderRegistry registry;
  private final SymmetricKeyGenerator symmetricKeyGenerator;
  private final ObjectMapper objectMapper;

  @Inject
  CreateTransferJobAction(
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
  public Class<CreateTransferJob> getRequestType() {
    return CreateTransferJob.class;
  }

  @Override
  public TransferJob handle(CreateTransferJob request) {
    // Create a new job and persist
    UUID jobId = UUID.randomUUID();

    String dataType = request.getDataType();
    String exportService = request.getExportService();
    String importService = request.getImportService();
    // TODO(rtannenbaum): Remove job_id param, this is for testing only
    String exportCallbackUrl = request.getExportCallbackUrl() + "?job_id=" + jobId.toString();
    String importCallbackUrl = request.getImportCallbackUrl();

    SecretKey sessionKey = symmetricKeyGenerator.generate();
    String encodedSessionKey = BaseEncoding.base64Url().encode(sessionKey.getEncoded());

    String encryptionScheme = request.getEncryptionScheme();
    PortabilityJob job =
        createJob(encodedSessionKey, dataType, exportService, importService, encryptionScheme);
    // TODO(rtannenbaum): Remove logging, for testing only
    logger.info("Created a new transfer job with id: {}, dataType: {}, exportService: {}, importService: {}, "
            + " encryptionScheme: {}", jobId, dataType, exportService, importService, encryptionScheme);

    AuthDataGenerator exportGenerator =
        registry.getAuthDataGenerator(job.exportService(), job.transferDataType(), EXPORT);
    Preconditions.checkNotNull(
        exportGenerator,
        "Generator not found for type: %s, service: %s",
        job.transferDataType(),
        job.exportService());

    AuthDataGenerator importGenerator =
        registry.getAuthDataGenerator(job.importService(), job.transferDataType(), IMPORT);
    Preconditions.checkNotNull(
        importGenerator,
        "Generator not found for type: %s, service: %s",
        job.transferDataType(),
        job.importService());

    try {
      jobStore.createJob(jobId, job);
      String encodedJobId = encodeJobId(jobId);

      AuthFlowConfiguration exportConfiguration =
          exportGenerator.generateConfiguration(exportCallbackUrl, encodedJobId);
      AuthFlowConfiguration importConfiguration =
          importGenerator.generateConfiguration(importCallbackUrl, encodedJobId);

      boolean jobNeedsUpdate = false;
      // If present, store initial auth data for export services, e.g. used for oauth1
      if (exportConfiguration.getInitialAuthData() != null) {
        jobNeedsUpdate = true;
        // Ensure initial auth data for export has not already been set
        Preconditions.checkState(
            Strings.isNullOrEmpty(job.jobAuthorization().encryptedInitialExportAuthData()));

        // Serialize and encrypt the initial auth data
        String serialized =
            objectMapper.writeValueAsString(exportConfiguration.getInitialAuthData());
        String encryptedInitialAuthData = EncrypterFactory.create(sessionKey).encrypt(serialized);

        // Add the serialized and encrypted initial auth data to the job authorization
        JobAuthorization updatedJobAuthorization =
            job.jobAuthorization()
                .toBuilder()
                .setEncryptedInitialExportAuthData(encryptedInitialAuthData)
                .build();

        // Persist the updated PortabilityJob with the updated JobAuthorization
        job = job.toBuilder().setAndValidateJobAuthorization(updatedJobAuthorization).build();
      }
      if (importConfiguration.getInitialAuthData() != null) {
        jobNeedsUpdate = true;

        // Ensure initial auth data for import has not already been set
        Preconditions.checkState(
            Strings.isNullOrEmpty(job.jobAuthorization().encryptedInitialImportAuthData()));

        // Serialize and encrypt the initial auth data
        String serialized =
            objectMapper.writeValueAsString(importConfiguration.getInitialAuthData());
        String encryptedInitialAuthData = EncrypterFactory.create(sessionKey).encrypt(serialized);

        // Add the serialized and encrypted initial auth data to the job authorization
        JobAuthorization updatedJobAuthorization =
            job.jobAuthorization()
                .toBuilder()
                .setEncryptedInitialImportAuthData(encryptedInitialAuthData)
                .build();

        // Persist the updated PortabilityJob with the updated JobAuthorization
        job = job.toBuilder().setAndValidateJobAuthorization(updatedJobAuthorization).build();
      }
      if (jobNeedsUpdate) {
        jobStore.updateJob(jobId, job);
      }

      return new TransferJob(
          encodedJobId,
          job.exportService(),
          job.importService(),
          job.transferDataType(),
          exportConfiguration.getAuthUrl(),
          importConfiguration.getAuthUrl(),
          exportConfiguration.getTokenUrl(),
          importConfiguration.getTokenUrl(),
          exportConfiguration.getAuthProtocol(),
          importConfiguration.getAuthProtocol());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /** Populates the initial state of the {@link PortabilityJob} instance. */
  private static PortabilityJob createJob(
      String encodedSessionKey,
      String dataType,
      String exportService,
      String importService,
      String encryptionScheme) {

    // Job auth data
    JobAuthorization jobAuthorization =
        JobAuthorization.builder()
            .setSessionSecretKey(encodedSessionKey)
            .setEncryptionScheme(encryptionScheme)
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
