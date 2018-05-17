package org.dataportabilityproject.api.action.transfer;

import com.google.api.client.util.Sleeper;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.io.BaseEncoding;
import com.google.inject.Inject;
import org.dataportabilityproject.api.action.Action;
import org.dataportabilityproject.api.action.ActionUtils;
import org.dataportabilityproject.security.AsymmetricKeyGenerator;
import org.dataportabilityproject.security.Encrypter;
import org.dataportabilityproject.security.EncrypterFactory;
import org.dataportabilityproject.security.SymmetricKeyGenerator;
import org.dataportabilityproject.spi.cloud.storage.JobStore;
import org.dataportabilityproject.spi.cloud.types.JobAuthorization;
import org.dataportabilityproject.spi.cloud.types.PortabilityJob;
import org.dataportabilityproject.types.client.transfer.StartTransfer;
import org.dataportabilityproject.types.client.transfer.Transfer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.security.PublicKey;
import java.util.UUID;

import static org.dataportabilityproject.api.action.ActionUtils.decodeJobId;
import static org.dataportabilityproject.spi.cloud.types.JobAuthorization.State.CREDS_AVAILABLE;
import static org.dataportabilityproject.spi.cloud.types.JobAuthorization.State.CREDS_ENCRYPTED;
import static org.dataportabilityproject.spi.cloud.types.JobAuthorization.State.CREDS_ENCRYPTION_KEY_GENERATED;

/** Starts a transfer process */
public class StartTransferAction implements Action<StartTransfer, Transfer> {

  private static final Logger logger = LoggerFactory.getLogger(StartTransferAction.class);

  private final JobStore jobStore;
  private final SymmetricKeyGenerator symmetricKeyGenerator;
  private final AsymmetricKeyGenerator asymmetricKeyGenerator;

  @Inject
  StartTransferAction(
      JobStore jobStore,
      SymmetricKeyGenerator symmetricKeyGenerator,
      AsymmetricKeyGenerator asymmetricKeyGenerator) {
    this.jobStore = jobStore;
    this.symmetricKeyGenerator = symmetricKeyGenerator;
    this.asymmetricKeyGenerator = asymmetricKeyGenerator;
  }

  @Override
  public Class<StartTransfer> getRequestType() {
    return StartTransfer.class;
  }

  @Override
  public Transfer handle(StartTransfer startTransfer) {
    String id = startTransfer.getId();
    UUID jobId = decodeJobId(id);

    // Update the job to indicate to transfer worker processes that creds are available for
    // encryption
    updateStateToCredsAvailable(jobId);

    // Poll and block until a public key is assigned to this job, e.g. from a specific transfer
    // worker
    // instance
    PortabilityJob job = pollForPublicKey(jobId);

    // Update this job with credentials encrypted with a public key, e.g. for a specific transfer
    // worker
    // instance
    encryptAndUpdateJobWithCredentials(
        jobId, job, startTransfer.getExportAuthData(), startTransfer.getImportAuthData());

    return null;
  }

  /**
   * Update the job to state to {@code State.CREDS_AVAILABLE} in the store. This indicates to the
   * pool of workers that this job is available for processing.
   */
  private void updateStateToCredsAvailable(UUID jobId) {
    PortabilityJob job = jobStore.findJob(jobId);
    validateJob(job);

    // Set update job auth data
    JobAuthorization jobAuthorization =
        job.jobAuthorization().toBuilder().setState(CREDS_AVAILABLE).build();

    job = job.toBuilder().setAndValidateJobAuthorization(jobAuthorization).build();
    try {
      jobStore.updateJob(jobId, job);
      logger.debug("Updated job {} to CREDS_AVAILABLE", jobId);
    } catch (IOException e) {
      throw new RuntimeException("Unable to update job", e);
    }
  }

  /**
   * Polls until the a public key is assigned and persisted with the job. This key will subsquently
   * be used to encrypt credentials.
   */
  private PortabilityJob pollForPublicKey(UUID jobId) {
    // Loop until the transfer worker updates it to assigned without auth data state, e.g. at that
    // point the transfer worker instance key will be populated
    // TODO: start new thread
    // TODO: implement timeout condition
    // TODO: Handle case where API dies while waiting
    PortabilityJob job = jobStore.findJob(jobId);
    while (job == null || job.jobAuthorization().state() != CREDS_ENCRYPTION_KEY_GENERATED) {
      logger.debug("Waiting for job {} to enter state CREDS_ENCRYPTION_KEY_GENERATED", jobId);
      try {
        Sleeper.DEFAULT.sleep(10000);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
      job = jobStore.findJob(jobId);
    }

    logger.debug("Got job {} in state CREDS_ENCRYPTION_KEY_GENERATED", jobId);

    // TODO: Consolidate validation with the internal PortabilityJob validation
    Preconditions.checkNotNull(
        job.jobAuthorization().authPublicKey(),
        "Expected job "
            + jobId
            + " to have a transfer worker instance's public key after being assigned "
            + "(state CREDS_ENCRYPTION_KEY_GENERATED)");
    Preconditions.checkState(
        job.jobAuthorization().encryptedExportAuthData() == null,
        "Didn't expect job "
            + jobId
            + " to have encrypted export auth data yet in state "
            + "CREDS_ENCRYPTION_KEY_GENERATED");
    Preconditions.checkState(
        job.jobAuthorization().encryptedImportAuthData() == null,
        "Didn't expect job "
            + jobId
            + " to have encrypted import auth data yet in state "
            + "CREDS_ENCRYPTION_KEY_GENERATED");

    return job;
  }

  // TODO: Consolidate validation with the internal PortabilityJob validation
  private void validateJob(PortabilityJob job) {

    // Validate
    String dataType = job.transferDataType();
    Preconditions.checkArgument(
        !Strings.isNullOrEmpty(dataType), "Missing valid dataTypeParam: %s", dataType);

    String exportService = job.exportService();
    Preconditions.checkArgument(
        ActionUtils.isValidExportService(exportService),
        "Missing valid exportService: %s",
        exportService);

    String importService = job.importService();
    Preconditions.checkArgument(
        ActionUtils.isValidImportService(importService),
        "Missing valid importService: %s",
        importService);
  }

  /**
   * Encrypt the export and import credentials with a new {@link SecretKey} and {@link PublicKey}
   * assigned to this job then update the data store to {@code State.CREDS_ENCRYPTED} state.
   */
  private void encryptAndUpdateJobWithCredentials(
      UUID jobId,
      PortabilityJob job,
      String encryptedExportAuthCredential,
      String encryptedImportAuthCredential) {

    // Step 1 - Generate authSecretKey, a new SecretKey which must not be persisted as is.
    SecretKey authSecretKey = symmetricKeyGenerator.generate();

    // Step 2 - Encrypt the auth data with authSecretKey
    Encrypter secretKeyEncrypter = EncrypterFactory.create(authSecretKey);
    String doublyEncryptedExportAuthData =
        secretKeyEncrypter.encrypt(encryptedExportAuthCredential);
    String doublyEncryptedImportAuthData =
        secretKeyEncrypter.encrypt(encryptedImportAuthCredential);

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
  }
}
