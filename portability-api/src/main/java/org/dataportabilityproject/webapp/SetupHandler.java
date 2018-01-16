/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.dataportabilityproject.webapp;

import static org.apache.axis.transport.http.HTTPConstants.HEADER_CONTENT_TYPE;
import static org.apache.axis.transport.http.HTTPConstants.HEADER_SET_COOKIE;
import static org.dataportabilityproject.webapp.PortabilityApiUtils.COOKIE_ATTRIBUTES;
import static org.dataportabilityproject.webapp.SetupHandler.Mode.IMPORT;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.net.HttpCookie;
import java.nio.charset.StandardCharsets;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonWriter;
import org.dataportabilityproject.ServiceProviderRegistry;
import org.dataportabilityproject.job.JobDao;
import org.dataportabilityproject.job.JobUtils;
import org.dataportabilityproject.job.PortabilityJob;
import org.dataportabilityproject.job.TokenManager;
import org.dataportabilityproject.shared.ServiceMode;
import org.dataportabilityproject.shared.auth.AuthFlowInitiator;
import org.dataportabilityproject.shared.auth.OnlineAuthDataGenerator;
import org.dataportabilityproject.shared.settings.CommonSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Common logic for job setup handlers.
 */
abstract class SetupHandler implements HttpHandler {

  private final Logger logger = LoggerFactory.getLogger(SetupHandler.class);

  private final JobDao jobDao;
  private final ServiceProviderRegistry serviceProviderRegistry;
  private final CommonSettings commonSettings;
  private final Mode mode;
  private final String handlerUrlPath;
  private final TokenManager tokenManager;

  protected SetupHandler(ServiceProviderRegistry serviceProviderRegistry,
      JobDao jobDao,
      CommonSettings commonSettings,
      Mode mode,
      String handlerUrlPath, TokenManager tokenManager) {
    this.jobDao = jobDao;
    this.serviceProviderRegistry = serviceProviderRegistry;
    this.commonSettings = commonSettings;
    this.mode = mode;
    this.handlerUrlPath = handlerUrlPath;
    this.tokenManager = tokenManager;
  }

  @Override
  public void handle(HttpExchange exchange) throws IOException {
    try {
      logger.debug("Entering setup handler, exchange: {}", exchange);
      Preconditions.checkArgument(
          PortabilityApiUtils.validateRequest(exchange, HttpMethods.GET, handlerUrlPath));

      String encodedIdCookie = PortabilityApiUtils
          .getCookie(exchange.getRequestHeaders(), JsonKeys.ID_COOKIE_KEY);
      Preconditions
          .checkArgument(!Strings.isNullOrEmpty(encodedIdCookie), "Encoded Id cookie required");

      // Valid job must be present
      String jobId = JobUtils.decodeId(encodedIdCookie);
      PortabilityJob job;
      if (commonSettings.getEncryptedFlow()) {
        job = PortabilityApiUtils.lookupJobPendingAuthData(jobId, jobDao);
      } else {
        job = PortabilityApiUtils.lookupJob(jobId, jobDao);
      }

      Preconditions.checkNotNull(job, "existingJob not found for jobId: %s", jobId);

      // This page is only valid after the oauth of the export service - export data should exist for
      // all setup Modes.
      String exportService = job.exportService();
      Preconditions.checkState(!Strings.isNullOrEmpty(exportService), "Export service is invalid");

      if (!commonSettings.getEncryptedFlow()) {
        Preconditions.checkNotNull(job.exportAuthData(), "Export AuthData is required");
      }

      String importService = job.importService();
      Preconditions.checkState(!Strings.isNullOrEmpty(importService), "Import service is invalid");

      JsonObject response;
      if (mode == IMPORT) {
        response = handleImportSetup(exchange.getRequestHeaders(), job, jobDao);
      } else {
        response = handleCopySetup(exchange.getRequestHeaders(), job);
        // Valid job is present, generate an XSRF token to pass back via cookie
        String tokenStr = tokenManager.createNewToken(jobId);
        HttpCookie token = new HttpCookie(JsonKeys.XSRF_TOKEN, tokenStr);
        exchange.getResponseHeaders().add(HEADER_SET_COOKIE, token.toString() + COOKIE_ATTRIBUTES);
      }

      // Mark the response as type Json and send
      exchange.getResponseHeaders()
          .set(HEADER_CONTENT_TYPE, "application/json; charset=" + StandardCharsets.UTF_8.name());
      exchange.sendResponseHeaders(200, 0);
      JsonWriter writer = Json.createWriter(exchange.getResponseBody());
      writer.write(response);
      writer.close();
    } catch (Exception e) {
      logger.error("Error handling request", e);
      throw e;
    }
  }

  private JsonObject handleImportSetup(Headers headers, PortabilityJob job, JobDao jobDao)
      throws IOException {
    if (!commonSettings.getEncryptedFlow()) {
      Preconditions.checkState(job.importAuthData() == null, "Import AuthData should not exist");
    } else {
      String exportAuthCookie = PortabilityApiUtils
          .getCookie(headers, JsonKeys.EXPORT_AUTH_DATA_COOKIE_KEY);
      Preconditions
          .checkArgument(!Strings.isNullOrEmpty(exportAuthCookie), "Export auth cookie required");
    }

    OnlineAuthDataGenerator generator = serviceProviderRegistry
        .getOnlineAuth(job.importService(), JobUtils.getDataType(job.dataType()), ServiceMode.IMPORT);
    AuthFlowInitiator authFlowInitiator = generator
        .generateAuthUrl(PortabilityApiFlags.baseApiUrl(), JobUtils.encodeId(job));

    // This is done in ConfigureHandler as well for export services
    if (authFlowInitiator.initialAuthData() != null) {
      // Auth data is different for import and export. This is only valid for the /_/importSetup page,
      // so serviceMode is IMPORT
      PortabilityJob updatedJob = JobUtils
          .setInitialAuthData(job, authFlowInitiator.initialAuthData(), ServiceMode.IMPORT);
      if (commonSettings.getEncryptedFlow()) {
        jobDao.updatePendingAuthDataJob(job);
      } else {
        jobDao.updateJob(updatedJob);
      }
    }
    return Json.createObjectBuilder().add(JsonKeys.DATA_TYPE, job.dataType())
        .add(JsonKeys.EXPORT_SERVICE, job.exportService())
        .add(JsonKeys.IMPORT_SERVICE, job.importService())
        .add(JsonKeys.IMPORT_AUTH_URL, authFlowInitiator.authUrl()).build();
  }

  JsonObject handleCopySetup(Headers requestHeaders, PortabilityJob job) {
    // Make sure the data exists in the cookies before rendering copy page
    if (commonSettings.getEncryptedFlow()) {
      String exportAuthCookie = PortabilityApiUtils
          .getCookie(requestHeaders, JsonKeys.EXPORT_AUTH_DATA_COOKIE_KEY);
      Preconditions
          .checkArgument(!Strings.isNullOrEmpty(exportAuthCookie), "Export auth cookie required");

      String importAuthCookie = PortabilityApiUtils
          .getCookie(requestHeaders, JsonKeys.IMPORT_AUTH_DATA_COOKIE_KEY);
      Preconditions
          .checkArgument(!Strings.isNullOrEmpty(importAuthCookie), "Import auth cookie required");
    } else {
      Preconditions.checkNotNull(job.importAuthData(), "Import AuthData is required");

    }

    return Json.createObjectBuilder().add(JsonKeys.DATA_TYPE, job.dataType())
        .add(JsonKeys.EXPORT_SERVICE, job.exportService())
        .add(JsonKeys.IMPORT_SERVICE, job.importService()).build();
  }


  // Which Setup flow to configure.
  // IMPORT mode sets up the import authorization flow.
  // COPY mode sets up the copy setup flow.
  public enum Mode {
    IMPORT, COPY
  }
}
