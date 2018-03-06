/*
 * Copyright 2017 The Data-Portability Project Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.dataportabilityproject.worker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import java.io.IOException;
import java.util.UUID;
import org.dataportabilityproject.security.Decrypter;
import org.dataportabilityproject.security.DecrypterFactory;
import org.dataportabilityproject.spi.cloud.storage.JobStore;
import org.dataportabilityproject.spi.cloud.types.JobAuthorization;
import org.dataportabilityproject.spi.cloud.types.PortabilityJob;
import org.dataportabilityproject.spi.transfer.InMemoryTransferCopier;
import org.dataportabilityproject.spi.transfer.provider.Exporter;
import org.dataportabilityproject.spi.transfer.provider.Importer;
import org.dataportabilityproject.spi.transfer.provider.TransferServiceProviderRegistry;
import org.dataportabilityproject.types.transfer.auth.AuthData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class WorkerImpl {
    private static final Logger logger = LoggerFactory.getLogger(WorkerImpl.class);

    private final JobPollingService jobPollingService;
    private final JobStore store;
    private final ObjectMapper objectMapper;
    private final WorkerJobMetadata workerJobMetadata;

    @Inject
    WorkerImpl(
        JobStore store,
        JobPollingService jobPollingService,
        ObjectMapper objectMapper,
        WorkerJobMetadata workerJobMetadata) {
        this.store = store;
        this.jobPollingService = jobPollingService;
        this.objectMapper = objectMapper;
        this.workerJobMetadata = workerJobMetadata;
    }

    /**
     * Start processing the job we've already polled.
     * TODO: Refactor this class as the polling and processing are completely separate
     */
    void processJob(Exporter<?, ?> exporter, Importer<?, ?> importer) {
        // Start the processing
        UUID jobId = workerJobMetadata.getJobId();
        logger.debug("Begin processing jobId: {}", jobId);
        PortabilityJob job = store.findJob(jobId);
        Preconditions.checkState(
                job.jobAuthorization().state() == JobAuthorization.State.CREDS_ENCRYPTED);

        processJob(jobId, job, exporter, importer);
        logger.info("Successfully processed jobId: {}", workerJobMetadata.getJobId());
    }

    /**
     * Start the polling service to poll for an unassigned job when it's ready.
     */
    void pollForJob() {
        jobPollingService.startAsync();
        jobPollingService.awaitTerminated();
    }

    /**
     * Get the metadata for the polled job.
     */
    WorkerJobMetadata getWorkerJobMetadata() {
        return workerJobMetadata;
    }

    private void processJob(UUID jobId, PortabilityJob job, Exporter exporter, Importer importer) {
        try {
            Decrypter decrypter =
                DecrypterFactory.create(workerJobMetadata.getKeyPair().getPrivate());
            JobAuthorization jobAuthorization = job.jobAuthorization();
            String serializedExportAuthData =
                    decrypter.decrypt(jobAuthorization.encryptedExportAuthData());
            AuthData exportAuthData = deSerialize(serializedExportAuthData);
            String serializedImportAuthData =
                    decrypter.decrypt(jobAuthorization.encryptedImportAuthData());
            AuthData importAuthData = deSerialize(serializedImportAuthData);
            InMemoryTransferCopier copier = new PortabilityInMemoryTransferCopier();
            copier.copy(exporter, importer, exportAuthData, importAuthData, jobId);
        } catch (IOException e) {
            logger.error("Error processing jobId: {}" + jobId, e);
        } finally {
            store.removeData(jobId);
        }
    }

    private AuthData deSerialize(String serialized) throws IOException {
        try {
            return objectMapper.readValue(serialized, AuthData.class);
        } catch (IOException e) {
            throw new IOException("Unable to deserialize AuthData", e);
        }
    }
}
