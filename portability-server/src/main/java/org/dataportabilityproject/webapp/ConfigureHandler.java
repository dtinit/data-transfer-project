package org.dataportabilityproject.webapp;

import static org.apache.axis.transport.http.HTTPConstants.HEADER_LOCATION;
import static org.apache.axis.transport.http.HTTPConstants.HEADER_SET_COOKIE;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.net.HttpCookie;
import java.util.Map;
import org.dataportabilityproject.ServiceProviderRegistry;
import org.dataportabilityproject.job.JobDao;
import org.dataportabilityproject.job.PortabilityJob;
import org.dataportabilityproject.job.PortabilityJobFactory;
import org.dataportabilityproject.shared.LogUtils;
import org.dataportabilityproject.shared.PortableDataType;
import org.dataportabilityproject.shared.auth.AuthFlowInitiator;
import org.dataportabilityproject.shared.auth.OnlineAuthDataGenerator;

/**
 * HttpHandler for the Configure service
 */
public class ConfigureHandler implements HttpHandler {

  private final ServiceProviderRegistry serviceProviderRegistry;
  private final JobDao jobDao;
  private final PortabilityJobFactory jobFactory;

  public ConfigureHandler(ServiceProviderRegistry serviceProviderRegistry,
                          JobDao jobDao,
                          PortabilityJobFactory jobFactory) {
    this.serviceProviderRegistry = serviceProviderRegistry;
    this.jobDao = jobDao;
    this.jobFactory = jobFactory;
  }

  /**
   * Given a set of job configuration parameters, this will create a new job and kick off auth flows
   * for the specified configuration. TODO: Determine what to do if previous job exists in the
   * session instead of creating a new job every time. TODO: Preconditions doesn't return an error
   * code or page. So if a page is requested with invalid params or incorrect method, no error is
   * present and the response is empty.
   */
  public void handle(HttpExchange exchange) throws IOException {
    Preconditions.checkArgument(
        PortabilityServerUtils.validateRequest(exchange, HttpMethods.POST, "/configure"),
        "/configure only supports POST.");

    Map<String, String> requestParameters = PortabilityServerUtils.getRequestParams(exchange);
    requestParameters.putAll(PortabilityServerUtils.getPostParams(exchange));

    Headers headers = exchange.getResponseHeaders();

    String dataTypeStr = requestParameters.get(JsonKeys.DATA_TYPE);
    Preconditions.checkArgument(!Strings.isNullOrEmpty(dataTypeStr),
        "Missing valid dataTypeParam: %s", dataTypeStr);

    PortableDataType dataType = JobUtils.getDataType(dataTypeStr);

    String exportService = requestParameters.get(JsonKeys.EXPORT_SERVICE);
    Preconditions.checkArgument(JobUtils.isValidService(exportService, true),
        "Missing valid exportService: %s", exportService);

    String importService = requestParameters.get(JsonKeys.IMPORT_SERVICE);
    Preconditions.checkArgument(JobUtils.isValidService(importService, false),
        "Missing valid importService: %s", importService);

    PortabilityJob newJob = jobFactory.create(dataType, exportService, importService);
    jobDao.insertJob(newJob);

    // Set new cookie
    HttpCookie cookie = new HttpCookie(JsonKeys.ID_COOKIE_KEY, JobUtils.encodeId(newJob));
    headers.add(HEADER_SET_COOKIE, cookie.toString());
    LogUtils.log("Set new cookie with key: %s, value: %s", JsonKeys.ID_COOKIE_KEY,
        JobUtils.encodeId(newJob));

    // Lookup job, even if just recently created
    PortabilityJob job = PortabilityServerUtils.lookupJob(newJob.id(), jobDao);
    Preconditions.checkState(job != null, "Job required");

    // TODO: Validate job before going further

    // Obtain the OnlineAuthDataGenerator
    OnlineAuthDataGenerator generator = serviceProviderRegistry
        .getOnlineAuth(job.exportService(), dataType);
    Preconditions.checkNotNull(generator, "Generator not found for type: %s, service: %s",
        dataType, job.exportService());

    // Auth authUrl
    AuthFlowInitiator authFlowInitiator = generator.generateAuthUrl(JobUtils.encodeId(newJob));
    Preconditions
        .checkNotNull(authFlowInitiator, "AuthFlowInitiator not found for type: %s, service: %s",
            dataType, job.exportService());

    // Store initial auth data
    if (authFlowInitiator.initialAuthData() != null) {
      PortabilityJob updatedJob = JobUtils
          .setInitialAuthData(job, authFlowInitiator.initialAuthData(), true);
      jobDao.updateJob(updatedJob);
    }

    // Send the authUrl for the client to redirect to service authorization
    // response.sendRedirect(authFlowInitiator.authUrl());
    LogUtils.log("Redirecting to: %s", authFlowInitiator.authUrl());
    headers.set(HEADER_LOCATION, authFlowInitiator.authUrl());
    exchange.sendResponseHeaders(303, -1);
  }
}
