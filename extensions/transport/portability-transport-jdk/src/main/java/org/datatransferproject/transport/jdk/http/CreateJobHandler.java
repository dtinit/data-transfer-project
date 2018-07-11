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
package org.datatransferproject.transport.jdk.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.io.BaseEncoding;
import com.google.common.net.HttpHeaders;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.datatransferproject.api.action.createjob.CreateJobAction;
import org.datatransferproject.api.action.createjob.CreateJobActionRequest;
import org.datatransferproject.api.action.createjob.CreateJobActionResponse;
import org.datatransferproject.api.launcher.TypeManager;
import org.datatransferproject.security.EncrypterFactory;
import org.datatransferproject.security.SymmetricKeyGenerator;
import org.datatransferproject.spi.api.auth.AuthDataGenerator;
import org.datatransferproject.spi.api.auth.AuthServiceProviderRegistry;
import org.datatransferproject.spi.api.auth.AuthServiceProviderRegistry.AuthMode;
import org.datatransferproject.spi.api.types.AuthFlowConfiguration;
import org.datatransferproject.spi.cloud.storage.JobStore;
import org.datatransferproject.spi.cloud.types.JobAuthorization;
import org.datatransferproject.spi.cloud.types.PortabilityJob;
import org.datatransferproject.types.client.transfer.CreateJobRequest;
import org.datatransferproject.types.client.transfer.CreateJobResponse;
import org.datatransferproject.types.client.transfer.CreateJobResponse.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.net.HttpCookie;
import java.nio.charset.StandardCharsets;

import static com.google.common.net.HttpHeaders.CONTENT_TYPE;
import static org.datatransferproject.api.action.ActionUtils.encodeJobId;

/**
 * HttpHandler for the {@link CreateJobAction}.
 */
final class CreateJobHandler implements HttpHandler {

  public static final String PATH = "/_/CreateJob";
  public static final String ERROR_PATH = "/error";
  private static final Logger logger = LoggerFactory.getLogger(CreateJobHandler.class);
  private final CreateJobAction createJobAction;
  private final AuthServiceProviderRegistry registry;
  private final JobStore store;
  private final SymmetricKeyGenerator symmetricKeyGenerator;
  private final ObjectMapper objectMapper;
  private final String baseApiUrl;

  @Inject
  CreateJobHandler(
      CreateJobAction createJobAction,
      AuthServiceProviderRegistry registry,
      JobStore store,
      TypeManager typeManager,
      SymmetricKeyGenerator symmetricKeyGenerator,
      @Named("baseApiUrl") String baseApiUrl) {
    this.createJobAction = createJobAction;
    this.registry = registry;
    this.store = store;
    this.symmetricKeyGenerator = symmetricKeyGenerator;
    this.objectMapper = typeManager.getMapper();
    this.baseApiUrl = baseApiUrl;
  }

  /** Services the {@link CreateJobAction} via the {@link HttpExchange}. */
  @Override
  public void handle(HttpExchange exchange) throws IOException {
    Preconditions.checkArgument(
        HandlerUtils.validateRequest(exchange, HandlerUtils.HttpMethods.POST, PATH),
        PATH + " only supports POST.");
    logger.debug("received request: {}", exchange.getRequestURI());
    CreateJobRequest request =
        objectMapper.readValue(exchange.getRequestBody(), CreateJobRequest.class);
    CreateJobActionRequest actionRequest =
        new CreateJobActionRequest(
            request.getSource(), request.getDestination(), request.getTransferDataType());
    CreateJobActionResponse actionResponse = createJobAction.handle(actionRequest);

    CreateJobResponse createJobResponse;
    if (actionResponse.getErrorMsg() != null) {
      logger.warn("Error during action: {}", actionResponse.getErrorMsg());
      handleError(exchange, request);
      return;
    }

    // Set new cookie
    String encodedJobId = encodeJobId(actionResponse.getId());
    HttpCookie cookie = new HttpCookie(JsonKeys.ID_COOKIE_KEY, encodedJobId);
    exchange
        .getResponseHeaders()
        .add(HttpHeaders.SET_COOKIE, cookie.toString() + HandlerUtils.COOKIE_ATTRIBUTES);

    // Initial auth flow url
    AuthDataGenerator generator =
        registry.getAuthDataGenerator(
            request.getSource(), request.getTransferDataType(), AuthMode.EXPORT);
    Preconditions.checkNotNull(
        generator,
        "Generator not found for type: %s, service: %s",
        request.getTransferDataType(),
        request.getSource());

    AuthFlowConfiguration authFlowConfiguration =
        generator.generateConfiguration(baseApiUrl, encodedJobId);
    Preconditions.checkNotNull(
        authFlowConfiguration,
        "AuthFlowConfiguration not found for type: %s, service: %s",
        request.getTransferDataType(),
        request.getSource());

    PortabilityJob job = store.findJob(actionResponse.getId());

    // If present, store initial auth data for export services, e.g. used for oauth1
    if (authFlowConfiguration.getInitialAuthData() != null) {

      // Retrieve and parse the session key from the job
      String sessionKey = job.jobAuthorization().sessionSecretKey();
      SecretKey key = symmetricKeyGenerator.parse(BaseEncoding.base64Url().decode(sessionKey));

      // Ensure initial auth data for export has not already been set
      Preconditions.checkState(
          Strings.isNullOrEmpty(job.jobAuthorization().encryptedInitialExportAuthData()));

      // Serialize and encrypt the initial auth data
      String serialized =
          objectMapper.writeValueAsString(authFlowConfiguration.getInitialAuthData());
      String encryptedInitialAuthData = EncrypterFactory.create(key).encrypt(serialized);

      // Add the serialized and encrypted initial auth data to the job authorization
      JobAuthorization updatedJobAuthorization =
          job.jobAuthorization()
              .toBuilder()
              .setEncryptedInitialExportAuthData(encryptedInitialAuthData)
              .build();

      // Persist the updated PortabilityJob with the updated JobAuthorization
      PortabilityJob updatedPortabilityJob =
          job.toBuilder().setAndValidateJobAuthorization(updatedJobAuthorization).build();

      store.updateJob(actionResponse.getId(), updatedPortabilityJob);
    }

    createJobResponse =
        new CreateJobResponse(
            request.getSource(),
            request.getDestination(),
            request.getTransferDataType(),
            Status.INPROCESS,
            authFlowConfiguration.getUrl());

    logger.debug("redirecting to: {}", authFlowConfiguration.getUrl());
    // Mark the response as type Json and send
    exchange
        .getResponseHeaders()
        .set(CONTENT_TYPE, "application/json; charset=" + StandardCharsets.UTF_8.name());
    exchange.sendResponseHeaders(200, 0);
    objectMapper.writeValue(exchange.getResponseBody(), createJobResponse);
  }

  /** Handles error response. TODO: Determine whether to return user facing error message here. */
  public void handleError(HttpExchange exchange, CreateJobRequest request) throws IOException {
    CreateJobResponse createJobResponse =
        new CreateJobResponse(
            request.getSource(),
            request.getDestination(),
            request.getTransferDataType(),
            Status.ERROR,
            ERROR_PATH);
    // Mark the response as type Json and send
    exchange
        .getResponseHeaders()
        .set(CONTENT_TYPE, "application/json; charset=" + StandardCharsets.UTF_8.name());
    exchange.sendResponseHeaders(200, 0);
    objectMapper.writeValue(exchange.getResponseBody(), createJobResponse);
  }
}
