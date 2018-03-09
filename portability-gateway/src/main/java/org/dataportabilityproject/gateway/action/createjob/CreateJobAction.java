/*
 * Copyright 2018 The Data Transfer Project Authors.
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
package org.dataportabilityproject.gateway.action.createjob;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.io.BaseEncoding;
import com.google.inject.Inject;
import java.io.IOException;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.dataportabilityproject.gateway.action.Action;
import org.dataportabilityproject.gateway.action.ActionUtils;
import org.dataportabilityproject.security.SymmetricKeyGenerator;
import org.dataportabilityproject.spi.cloud.storage.JobStore;
import org.dataportabilityproject.spi.cloud.types.JobAuthorization;
import org.dataportabilityproject.spi.cloud.types.PortabilityJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** An {@link Action} that handles the initial creation of a job. */
public final class CreateJobAction
    implements Action<CreateJobActionRequest, CreateJobActionResponse> {

  private static final Logger logger = LoggerFactory.getLogger(CreateJobAction.class);

  private final JobStore store;
  private final SymmetricKeyGenerator symmetricKeyGenerator;

  @Inject
  CreateJobAction(JobStore store, SymmetricKeyGenerator symmetricKeyGenerator) {
    this.store = store;
    this.symmetricKeyGenerator = symmetricKeyGenerator;
  }

  /** Populates the initial state of the {@link PortabilityJob} instance. */
  private static PortabilityJob createJob(
      String encodedSessionKey, String dataType, String exportService, String importService) {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(encodedSessionKey), "sessionKey missing");
    Preconditions.checkArgument(!Strings.isNullOrEmpty(exportService), "exportService missing");
    Preconditions.checkArgument(!Strings.isNullOrEmpty(importService), "importService missing");
    Preconditions.checkNotNull(dataType, "dataType missing");

    // Job auth data
    JobAuthorization jobAuthorization =
        JobAuthorization.builder()
            .setEncryptedSessionKey(encodedSessionKey)
            .setState(JobAuthorization.State.INITIAL)
            .build();

    return PortabilityJob.builder()
        .setTransferDataType(dataType)
        .setExportService(exportService)
        .setImportService(importService)
        .setAndValidateJobAuthorization(jobAuthorization)
        .build();
  }

  /**
   * Given a set of job configuration parameters, this will create a new job and kick off auth flows
   * for the specified configuration. TODO: Determine what to do if previous job exists in the
   * session instead of creating a new job every time. TODO: Preconditions doesn't return an error
   * code or page. So if a page is requested with invalid params or incorrect method, no error is
   * present and the response is empty.
   */
  @Override
  public CreateJobActionResponse handle(CreateJobActionRequest request) {

    String dataType = request.getTransferDataType();
    Preconditions.checkArgument(
        !Strings.isNullOrEmpty(dataType), "Missing valid dataTypeParam: %s", dataType);

    String exportService = request.getSource();
    Preconditions.checkArgument(
        ActionUtils.isValidExportService(exportService),
        "Missing valid exportService: %s",
        exportService);

    String importService = request.getDestination();
    Preconditions.checkArgument(
        ActionUtils.isValidImportService(importService),
        "Missing valid importService: %s",
        importService);

    // Create a new job and persist
    UUID newId = UUID.randomUUID();
    SecretKey sessionKey = symmetricKeyGenerator.generate();
    String encodedSessionKey = BaseEncoding.base64Url().encode(sessionKey.getEncoded());

    PortabilityJob job = createJob(encodedSessionKey, dataType, exportService, importService);
    try {
      store.createJob(newId, job);
    } catch (IOException e) {
      logger.warn("Error creating job with id: {}", newId, e);
      return CreateJobActionResponse.createWithError("Unable to create a new job");
    }
    return CreateJobActionResponse.create(newId);
  }

  /**
   * Creates the id and session key for a new job and creates the {@link PortabilityJob} instance.
   */
  private PortabilityJob createJob(String dataType, String exportService, String importService) {
    // Create a new job and persist
    String newId = UUID.randomUUID().toString();
    SecretKey sessionKey = symmetricKeyGenerator.generate();
    String encodedSessionKey = BaseEncoding.base64Url().encode(sessionKey.getEncoded());
    return createJob(encodedSessionKey, dataType, exportService, importService);
  }
}
