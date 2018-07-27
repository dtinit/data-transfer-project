package org.datatransferproject.api.action.transfer;

import com.google.common.io.BaseEncoding;
import com.google.inject.Inject;
import org.datatransferproject.api.action.Action;
import org.datatransferproject.security.AsymmetricKeyGenerator;
import org.datatransferproject.security.Encrypter;
import org.datatransferproject.security.EncrypterFactory;
import org.datatransferproject.security.SymmetricKeyGenerator;
import org.datatransferproject.spi.cloud.storage.JobStore;
import org.datatransferproject.spi.cloud.types.JobAuthorization;
import org.datatransferproject.spi.cloud.types.PortabilityJob;
import org.datatransferproject.types.client.transfer.StartTransferJob;
import org.datatransferproject.types.client.transfer.TransferJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.security.PublicKey;
import java.util.UUID;

import static org.datatransferproject.api.action.ActionUtils.decodeJobId;
import static org.datatransferproject.spi.cloud.types.JobAuthorization.State.CREDS_ENCRYPTED;

/** Starts a transfer job. */
public class StartTransferJobAction implements Action<StartTransferJob, TransferJob> {
  private static final Logger logger = LoggerFactory.getLogger(StartTransferJobAction.class);

  private final JobStore jobStore;
  private final SymmetricKeyGenerator symmetricKeyGenerator;
  private final AsymmetricKeyGenerator asymmetricKeyGenerator;

  @Inject
  StartTransferJobAction(
      JobStore jobStore,
      SymmetricKeyGenerator symmetricKeyGenerator,
      AsymmetricKeyGenerator asymmetricKeyGenerator) {
    this.jobStore = jobStore;
    this.symmetricKeyGenerator = symmetricKeyGenerator;
    this.asymmetricKeyGenerator = asymmetricKeyGenerator;
  }

  @Override
  public Class<StartTransferJob> getRequestType() {
    return StartTransferJob.class;
  }

  @Override
  public TransferJob handle(StartTransferJob startTransferJob) {
    String id = startTransferJob.getId();
    UUID jobId = decodeJobId(id);
    PortabilityJob job = jobStore.findJob(jobId);

    // TODO: move creds encryption to the client, and pass encrypted creds to this action.
    // Update this job with credentials encrypted with a public key, e.g. for a specific transfer
    // worker instance
    job = encryptAndUpdateJobWithCredentials(
        jobId, job, startTransferJob.getExportAuthData(), startTransferJob.getImportAuthData());

    return new TransferJob(id, job.exportService(), job.importService(), job.transferDataType(), null, null);
  }

  /**
   * Encrypt the export and import credentials with a new {@link SecretKey} and {@link PublicKey}
   * assigned to this job then update the data store to {@code State.CREDS_ENCRYPTED} state.
   */
  private PortabilityJob encryptAndUpdateJobWithCredentials(
      UUID jobId,
      PortabilityJob job,
      String exportAuthData,
      String importAuthData) {

    // Step 1 - Generate authSecretKey, a new SecretKey which must not be persisted as is.
    SecretKey authSecretKey = symmetricKeyGenerator.generate();

    // Step 2 - Encrypt the auth data with authSecretKey
    Encrypter secretKeyEncrypter = EncrypterFactory.create(authSecretKey);
    String doublyEncryptedExportAuthData =
        secretKeyEncrypter.encrypt(exportAuthData);
    String doublyEncryptedImportAuthData =
        secretKeyEncrypter.encrypt(importAuthData);

    // Step 3 - Encrypt the authSecretKey itself with the authPublickey
    PublicKey authPublicKey =
        asymmetricKeyGenerator.parse(
            BaseEncoding.base64Url().decode(job.jobAuthorization().authPublicKey()));
    Encrypter asymmetricEncrypter = EncrypterFactory.create(authPublicKey);

    String encryptedAuthSecretKey =
        asymmetricEncrypter.encrypt(BaseEncoding.base64Url().encode(authSecretKey.getEncoded()));

    // Populate job with encrypted auth data
    JobAuthorization updatedJobAuthorization =
        job.jobAuthorization()
            .toBuilder()
            .setEncryptedExportAuthData(doublyEncryptedExportAuthData)
            .setEncryptedImportAuthData(doublyEncryptedImportAuthData)
            .setAuthSecretKey(encryptedAuthSecretKey)
            .setState(CREDS_ENCRYPTED)
            .build();
    job = job.toBuilder().setAndValidateJobAuthorization(updatedJobAuthorization).build();
    logger.debug("Updating job {} from CREDS_ENCRYPTION_KEY_GENERATED to CREDS_ENCRYPTED", jobId);
    try {
      jobStore.updateJob(jobId, job);
      logger.debug("Updated job {} to CREDS_ENCRYPTED", jobId);
    } catch (IOException e) {
      throw new RuntimeException("Unable to update job", e);
    }
    return job;
  }
}
