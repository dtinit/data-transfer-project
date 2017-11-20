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
import org.dataportabilityproject.shared.LogUtils;
import org.dataportabilityproject.shared.auth.AuthFlowInitiator;
import org.dataportabilityproject.shared.auth.OnlineAuthDataGenerator;

public class CopySetupHandler implements HttpHandler {

  private final ServiceProviderRegistry serviceProviderRegistry;
  private final JobDao jobDao;

  public CopySetupHandler(ServiceProviderRegistry serviceProviderRegistry, JobDao jobDao) {
    this.serviceProviderRegistry = serviceProviderRegistry;
    this.jobDao = jobDao;
  }

  public void handle(HttpExchange exchange) throws IOException {
    Preconditions.checkArgument(
        PortabilityServerUtils.validateRequest(exchange, HttpMethods.GET, "/_/copySetup"));

    String encodedIdCookie = PortabilityServerUtils
        .getCookie(exchange.getRequestHeaders(), JsonKeys.ID_COOKIE_KEY);
    Preconditions
        .checkArgument(!Strings.isNullOrEmpty(encodedIdCookie), "Encoded Id cookie required");

    // Valid job must be present
    String jobId = JobUtils.decodeId(encodedIdCookie);
    PortabilityJob job = jobDao.findExistingJob(jobId);
    Preconditions.checkState(null != job, "existingJob not found for id: %s", jobId);

    String exportService = job.exportService();
    Preconditions.checkState(!Strings.isNullOrEmpty(exportService), "Export service is invalid");
    Preconditions.checkState(job.exportAuthData() != null, "Export AuthData is required");

    String importService = job.importService();
    Preconditions.checkState(!Strings.isNullOrEmpty(importService), "Import service is invalid");
    Preconditions.checkState(job.importAuthData() != null, "Import AuthData is required");

    LogUtils.log("copySetup, importService: %s, exportService %s", importService, exportService);

    // Obtain the OnlineAuthDataGenerator
    OnlineAuthDataGenerator generator = serviceProviderRegistry
        .getOnlineAuth(job.importService(), JobUtils.getDataType(job.dataType()));
    AuthFlowInitiator authFlowInitiator = generator.generateAuthUrl(JobUtils.encodeId(job));

    // Store authUrl
    if (authFlowInitiator.initialAuthData() != null) {
      PortabilityJob updatedJob = JobUtils
          .setInitialAuthData(job, authFlowInitiator.initialAuthData(), true);
      jobDao.updateJob(updatedJob);
    }
    LogUtils.log("copySetup, import auth authUrl sent to client: %s", authFlowInitiator.authUrl());

    JsonObject response = PortabilityServerUtils
        .createImportAuthJobResponse(job.dataType(), exportService,
            importService, authFlowInitiator.authUrl());
    LogUtils.log("copySetup, response: %s", response.toString());

    // Mark the response as type Json and send
    exchange.getResponseHeaders()
        .set(HEADER_CONTENT_TYPE, "application/json; charset=" + StandardCharsets.UTF_8.name());
    exchange.sendResponseHeaders(200, 0);
    JsonWriter writer = Json.createWriter(exchange.getResponseBody());
    writer.write(response);
    writer.close();
  }
}
