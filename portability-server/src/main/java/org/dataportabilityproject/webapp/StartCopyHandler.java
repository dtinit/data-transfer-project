package org.dataportabilityproject.webapp;

import static org.apache.axis.transport.http.HTTPConstants.HEADER_CONTENT_TYPE;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonWriter;
import org.dataportabilityproject.PortabilityCopier;
import org.dataportabilityproject.ServiceProviderRegistry;
import org.dataportabilityproject.cloud.interfaces.CloudFactory;
import org.dataportabilityproject.job.JobManager;
import org.dataportabilityproject.job.PortabilityJob;
import org.dataportabilityproject.shared.PortableDataType;

public class StartCopyHandler implements HttpHandler {

  private final ServiceProviderRegistry serviceProviderRegistry;
  private final JobManager jobManager;
  private final CloudFactory cloudFactory;

  public StartCopyHandler(ServiceProviderRegistry serviceProviderRegistry, JobManager jobManager,
      CloudFactory cloudFactory) {
    this.serviceProviderRegistry = serviceProviderRegistry;
    this.jobManager = jobManager;
    this.cloudFactory = cloudFactory;
  }

  public void handle(HttpExchange exchange) throws IOException {
    Preconditions.checkArgument(
        PortabilityServerUtils.validateRequest(exchange, HttpMethods.GET, "/_/startCopy"));

    String encodedIdCookie = PortabilityServerUtils
        .getCookie(exchange.getRequestHeaders(), JsonKeys.ID_COOKIE_KEY);
    Preconditions
        .checkArgument(!Strings.isNullOrEmpty(encodedIdCookie), "Encoded Id cookie required");

    // Valid job must be present
    String jobId = JobUtils.decodeId(encodedIdCookie);
    PortabilityJob job = jobManager.findExistingJob(jobId);
    Preconditions.checkState(null != job, "existingJob not found for id: %s", jobId);

    String exportService = job.exportService();
    Preconditions.checkState(!Strings.isNullOrEmpty(exportService), "Export service is invalid");
    Preconditions.checkState(job.exportAuthData() != null, "Export AuthData is required");

    String importService = job.importService();
    Preconditions.checkState(!Strings.isNullOrEmpty(importService), "Import service is invalid");
    Preconditions.checkState(job.importAuthData() != null, "Import AuthData is required");

    LogUtils.log("startCopy, importService: %s, exportService %s", importService, exportService);
    PortableDataType type = JobUtils.getDataType(job.dataType());

    // TODO: Design better threading for new copy tasks with exception handling
    Runnable r = new Runnable() {
      public void run() {
        try {
          PortabilityCopier.copyDataType(serviceProviderRegistry, type, exportService,
              job.exportAuthData(), importService, job.importAuthData(), job.id());
        } catch (IOException e) {
          System.out.println("copyDataType failed");
          e.printStackTrace();
        } finally {
          cloudFactory.clearJobData(job.id());
        }
      }
    };

    ExecutorService executor = Executors.newCachedThreadPool();
    executor.submit(r);

    JsonObject response = Json.createObjectBuilder().add("status", "started").build();
    LogUtils.log("startCopy, response: %s", response.toString());

    // Mark response as Json and send
    exchange.getResponseHeaders()
        .set(HEADER_CONTENT_TYPE, "application/json; charset=" + StandardCharsets.UTF_8.name());
    exchange.sendResponseHeaders(200, 0);
    JsonWriter writer = Json.createWriter(exchange.getResponseBody());
    writer.write(response);
    writer.close();
  }
}
