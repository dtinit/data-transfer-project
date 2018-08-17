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

import javax.crypto.SecretKey;
import java.io.IOException;
import java.util.UUID;

import static org.datatransferproject.api.action.ActionUtils.encodeJobId;
import static org.datatransferproject.spi.api.auth.AuthServiceProviderRegistry.AuthMode.EXPORT;
import static org.datatransferproject.spi.api.auth.AuthServiceProviderRegistry.AuthMode.IMPORT;

/** Creates a transfer job and prepares it for both the export and import service authentication flow. */
public class CreateTransferJobAction implements Action<CreateTransferJob, TransferJob> {
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
        String dataType = request.getDataType();
        String exportService = request.getExportService();
        String importService = request.getImportService();
        String callbackUrl = request.getCallbackUrl();

        // Create a new job and persist
        UUID jobId = UUID.randomUUID();
        SecretKey sessionKey = symmetricKeyGenerator.generate();
        String encodedSessionKey = BaseEncoding.base64Url().encode(sessionKey.getEncoded());

        PortabilityJob job = createJob(encodedSessionKey, dataType, exportService, importService);

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
                    exportGenerator.generateConfiguration(callbackUrl, encodedJobId);
            AuthFlowConfiguration importConfiguration =
                    importGenerator.generateConfiguration(callbackUrl, encodedJobId);

            boolean jobNeedsUpdate = false;
            // If present, store initial auth data for export services, e.g. used for oauth1
            if (exportConfiguration.getInitialAuthData() != null) {
                jobNeedsUpdate = true;
                // Ensure initial auth data for export has not already been set
                Preconditions.checkState(
                        Strings.isNullOrEmpty(job.jobAuthorization().encryptedInitialExportAuthData()));

                // Serialize and encrypt the initial auth data
                String serialized = objectMapper.writeValueAsString(exportConfiguration.getInitialAuthData());
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
                String serialized = objectMapper.writeValueAsString(importConfiguration.getInitialAuthData());
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

            return new TransferJob(encodedJobId, job.exportService(), job.importService(), job.transferDataType(),
                    exportConfiguration.getAuthUrl(), importConfiguration.getAuthUrl(),
                    exportConfiguration.getTokenUrl(), importConfiguration.getTokenUrl(),
                    exportConfiguration.getAuthProtocol(), importConfiguration.getAuthProtocol());
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
