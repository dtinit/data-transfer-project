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

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonWriter;
import org.dataportabilityproject.ServiceProviderRegistry;
import org.dataportabilityproject.job.JobDao;
import org.dataportabilityproject.job.PortabilityJob;
import org.dataportabilityproject.shared.auth.AuthFlowInitiator;
import org.dataportabilityproject.shared.auth.OnlineAuthDataGenerator;

/**
 * HttpHandler for ImportSetup service
 */
public class ImportSetupHandler implements HttpHandler {

  private final JobDao jobDao;
  private final ServiceProviderRegistry serviceProviderRegistry;

  public ImportSetupHandler(ServiceProviderRegistry serviceProviderRegistry,
      JobDao jobDao) {
    this.jobDao = jobDao;
    this.serviceProviderRegistry = serviceProviderRegistry;

  }

  public void handle(HttpExchange exchange) throws IOException {
    Preconditions.checkArgument(
        PortabilityServerUtils.validateRequest(exchange, HttpMethods.GET, "/_/importSetup"));

    String encodedIdCookie = PortabilityServerUtils
        .getCookie(exchange.getRequestHeaders(), JsonKeys.ID_COOKIE_KEY);
    Preconditions
        .checkArgument(!Strings.isNullOrEmpty(encodedIdCookie), "Encoded Id cookie required");

    // Valid job must be present
    String jobId = JobUtils.decodeId(encodedIdCookie);
    PortabilityJob job = jobDao.findExistingJob(jobId);

    Preconditions.checkState(null != job, "existingJob not found for jobId: %s", jobId);

    String exportService = job.exportService();
    String importService = job.importService();

    Preconditions.checkState(!Strings.isNullOrEmpty(exportService), "Export service is invalid");
    Preconditions.checkState(job.exportAuthData() != null, "Export AuthData is required");

    Preconditions.checkState(!Strings.isNullOrEmpty(importService), "Import service is invalid");
    // TODO: ensure import auth data doesn't exist when this is called in the auth flow
    Preconditions.checkState(job.importAuthData() == null, "Import AuthData should not exist");

    OnlineAuthDataGenerator generator = serviceProviderRegistry
        .getOnlineAuth(job.importService(), JobUtils.getDataType(job.dataType()));
    AuthFlowInitiator authFlowInitiator = generator.generateAuthUrl(JobUtils.encodeId(job));

    // Store initial auth data - this page only is valid for import services, so isExport is set to false.
    // TODO: support both import and export.
    if (authFlowInitiator.initialAuthData() != null) {
      PortabilityJob updatedJob = JobUtils
          .setInitialAuthData(job, authFlowInitiator.initialAuthData(), false);
      jobDao.updateJob(updatedJob);

    }

    JsonObject response = PortabilityServerUtils
        .createImportAuthJobResponse(job.dataType(), job.exportService(),
            job.importService(), authFlowInitiator.authUrl());

    // Mark the response as type Json and send
    exchange.getResponseHeaders()
        .set(HEADER_CONTENT_TYPE, "application/json; charset=" + StandardCharsets.UTF_8.name());
    exchange.sendResponseHeaders(200, 0);
    JsonWriter writer = Json.createWriter(exchange.getResponseBody());
    writer.write(response);
    writer.close();
  }
}
