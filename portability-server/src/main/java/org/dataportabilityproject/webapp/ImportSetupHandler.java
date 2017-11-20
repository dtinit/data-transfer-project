package org.dataportabilityproject.webapp;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.net.HttpCookie;
import java.util.Map;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonWriter;
import org.dataportabilityproject.ServiceProviderRegistry;
import org.dataportabilityproject.job.JobManager;
import org.dataportabilityproject.job.PortabilityJob;
import org.dataportabilityproject.shared.auth.AuthFlowInitiator;
import org.dataportabilityproject.shared.auth.OnlineAuthDataGenerator;

/**
 * HttpHandler for ImportSetup service
 */
public class ImportSetupHandler implements HttpHandler {

  private final JobManager jobManager;
  private final ServiceProviderRegistry serviceProviderRegistry;

  public ImportSetupHandler(ServiceProviderRegistry serviceProviderRegistry,
      JobManager jobManager) {
    this.jobManager = jobManager;
    this.serviceProviderRegistry = serviceProviderRegistry;

  }

  public void handle(HttpExchange exchange) throws IOException {
    Preconditions.checkArgument(
        PortabilityServerUtils.validateRequest(exchange, HttpMethods.GET, "/_/importSetup"));
    Headers requestHeaders = exchange.getRequestHeaders();

    Map<String, HttpCookie> httpCookies = PortabilityServerUtils.getCookies(requestHeaders);
    HttpCookie encodedIdCookie = httpCookies.get(JsonKeys.ID_COOKIE_KEY);
    Preconditions
        .checkArgument(
            encodedIdCookie != null && !Strings.isNullOrEmpty(encodedIdCookie.getValue()),
            "Encoded Id cookie required");

    // Valid job must be present
    String jobId = JobUtils.decodeId(encodedIdCookie.getValue());
    PortabilityJob job = jobManager.findExistingJob(jobId);
    Preconditions.checkState(null != job, "existingJob not found for jobId: %s", jobId);

    LogUtils.log("importSetup, job: %s", job);

    String exportService = job.exportService();
    String importService = job.importService();

    Preconditions.checkState(!Strings.isNullOrEmpty(exportService), "Export service is invalid");
    Preconditions.checkState(job.exportAuthData() != null, "Export AuthData is required");

    Preconditions.checkState(!Strings.isNullOrEmpty(importService), "Import service is invalid");
    // TODO: ensure import auth data doesn't exist when this is called in the auth flow
    Preconditions.checkState(job.importAuthData() == null, "Import AuthData should not exist");

    LogUtils.log("importSetup, importService: %s, exportService: %s", importService, exportService);

    OnlineAuthDataGenerator generator = serviceProviderRegistry
        .getOnlineAuth(job.importService(), JobUtils.getDataType(job.dataType()));
    AuthFlowInitiator authFlowInitiator = generator.generateAuthUrl(JobUtils.encodeId(job));

    // Store authUrl
    if (authFlowInitiator.initialAuthData() != null) {
      PortabilityJob updatedJob = JobUtils
          .setInitialAuthData(job, authFlowInitiator.initialAuthData(), true);
      jobManager.updateJob(updatedJob);
    }

    JsonObject response = generateJsonResponse(job.dataType(), job.exportService(),
        job.importService(), authFlowInitiator.authUrl());
    LogUtils.log("importSetup, response: %s", response.toString());

    // Send response
    exchange.sendResponseHeaders(200, 0);
    JsonWriter writer = Json.createWriter(exchange.getResponseBody());
    writer.write(response);
    writer.close();


  }

  JsonObject generateJsonResponse(String dataType, String exportService, String importService,
      String importAuthURL) {
    return Json.createObjectBuilder().add(JsonKeys.DATA_TYPE, dataType)
        .add(JsonKeys.EXPORT_SERVICE, exportService)
        .add(JsonKeys.IMPORT_SERVICE, importService)
        .add(JsonKeys.IMPORT_AUTH_URL, importAuthURL).build();
  }
}
