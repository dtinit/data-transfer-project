package org.dataportabilityproject.webapp;

import static org.apache.axis.transport.http.HTTPConstants.HEADER_LOCATION;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.util.Map;
import org.dataportabilityproject.ServiceProviderRegistry;
import org.dataportabilityproject.job.JobDao;
import org.dataportabilityproject.job.PortabilityJob;
import org.dataportabilityproject.shared.LogUtils;
import org.dataportabilityproject.shared.PortableDataType;
import org.dataportabilityproject.shared.Secrets;
import org.dataportabilityproject.shared.auth.AuthData;
import org.dataportabilityproject.shared.auth.OnlineAuthDataGenerator;

/**
 * HttpHandler for SimpleLoginSubmit Controller.
 */
public class SimpleLoginSubmitHandler implements HttpHandler {

  private final ServiceProviderRegistry serviceProviderRegistry;
  private final JobDao jobDao;
  private final CryptoHelper cryptoHelper;

  public SimpleLoginSubmitHandler(ServiceProviderRegistry serviceProviderRegistry, JobDao jobDao,
      CryptoHelper cryptoHelper) {
    this.serviceProviderRegistry = serviceProviderRegistry;
    this.jobDao = jobDao;
    this.cryptoHelper = cryptoHelper;
  }

  public void handle(HttpExchange exchange) throws IOException {
    PortabilityServerUtils.validateRequest(exchange, HttpMethods.POST, "/simpleLoginSubmit");

    String encodedIdCookie = PortabilityServerUtils
        .getCookie(exchange.getRequestHeaders(), JsonKeys.ID_COOKIE_KEY);
    Preconditions
        .checkArgument(!Strings.isNullOrEmpty(encodedIdCookie), "Encoded Id Cookie required");
    String jobId = JobUtils.decodeId(encodedIdCookie);

    PortabilityJob job = jobDao.findExistingJob(jobId);
    Preconditions.checkState(null != job, "existingJob not found for job id: %s", jobId);

    // TODO: Determine import vs export mode
    // Hack! For now, if we don't have export auth data, assume it's for export.
    boolean isExport = (null == job.exportAuthData());
    String service = isExport ? job.exportService() : job.importService();
    Preconditions.checkState(!Strings.isNullOrEmpty(service),
        "service not found, service: %s isExport: %b, job id: %s", service, isExport, jobId);

    PortableDataType dataType = JobUtils.getDataType(job.dataType());

    LogUtils.log("simpleLoginSubmit, job: %s", job);
    LogUtils.log("simpleLoginSubmit, dataType: %s", dataType);
    LogUtils.log("service: %s, isExport: %b", service, isExport);

    Map<String, String> requestParams = PortabilityServerUtils.getRequestParams(exchange);
    requestParams.putAll(PortabilityServerUtils.getPostParams(exchange));

    String username = requestParams.get("username");
    String password = requestParams.get("password");
    Preconditions
        .checkArgument(!Strings.isNullOrEmpty(username), "Missing valid username: %s", username);
    Preconditions.checkArgument(!Strings.isNullOrEmpty(password), "Password is empty");

    OnlineAuthDataGenerator generator = serviceProviderRegistry
        .getOnlineAuth(job.exportService(), dataType);
    Preconditions.checkNotNull(generator, "Generator not found for type: %s, service: %s",
        dataType, job.exportService());

    // Generate and store auth data
    AuthData authData = generator.generateAuthData(username, jobId, null, password);
    Preconditions.checkNotNull(authData, "Auth data should not be null");

    // Update the job
    // TODO: Remove persistence of auth data in storage at this point
    PortabilityJob updatedJob = JobUtils.setAuthData(job, authData, isExport);
    jobDao.updateJob(updatedJob);

    String redirect = Secrets.getInstance().baseUrl() + (isExport ? "/next" : "/copy");
    // Set new cookie and redirect to the next page
    LogUtils.log("simpleLoginSubmit, redirecting to: %s", redirect);
    Headers responseHeaders = exchange.getResponseHeaders();
    cryptoHelper.encryptAndSetCookie(responseHeaders, isExport, authData);
    responseHeaders.set(HEADER_LOCATION, redirect);
    exchange.sendResponseHeaders(303, -1);
  }
}
