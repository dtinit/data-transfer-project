package org.dataportabilityproject.webapp;

import com.google.api.client.auth.oauth2.AuthorizationCodeResponseUrl;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.dataportabilityproject.ServiceProviderRegistry;
import org.dataportabilityproject.shared.Config;
import org.dataportabilityproject.shared.PortableDataType;
import org.dataportabilityproject.shared.auth.AuthData;
import org.dataportabilityproject.shared.auth.OnlineAuthDataGenerator;
import org.dataportabilityproject.webapp.job.JobManager;
import org.dataportabilityproject.webapp.job.PortabilityJob;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Controller for the list data types service. */
@RestController
public class Oauth2CallbackController {
  @Autowired
  private ServiceProviderRegistry serviceProviderRegistry;
  @Autowired
  private JobManager jobManager;

  /** Handle Oauth2 callback requests. */
  @RequestMapping("/callback/**")
  public void handleOauth2Response(
      @CookieValue(value = "jobToken", required = true) String token,
      HttpServletRequest request,
      HttpServletResponse response) throws Exception {
    LogUtils.log("Oauth2CallbackController getRequestURI: %s", request.getRequestURI());
    LogUtils.log("Oauth2CallbackController getQueryString: %s", request.getQueryString());

    AuthorizationCodeResponseUrl authResponse = getResponseUrl(request);

    // check for user-denied error
    if (authResponse.getError() != null) {
      LogUtils.log("Authorization DENIED: %s", authResponse.getError());
      response.sendRedirect("/error");
      return;
    }

    Preconditions.checkArgument(!Strings.isNullOrEmpty(token), "Token required");

    // TODO: Encrypt/decrypt state param with secure info
    String state = authResponse.getState();

    // TODO: Remove sanity check
    Preconditions.checkState(state.equals(token), "Token in cookie [%s] and request [%s] should match", token, state);

    PortabilityJob job = lookupJob(token);
    PortableDataType dataType = JobUtils.getDataType(job.dataType());
    LogUtils.log("dataType: %s", dataType);

    // TODO: Determine import vs export mode
    // Hack! For now, if we don't have export auth data, assume it's for export.
    boolean isExport = (null == job.exportAuthData());

    // TODO: Determine service from job or from authUrl path?
    String service = isExport ? job.exportService() : job.importService();
    Preconditions.checkState(!Strings.isNullOrEmpty(service), "service not found, service: %s isExport: %b, token: %s", service, isExport, token);
    LogUtils.log("service: %s, isExport: %b", service, isExport);

    // Obtain the ServiceProvider from the registry
    OnlineAuthDataGenerator generator = serviceProviderRegistry.getOnlineAuth(service, dataType);

    // Retrieve initial auth data, if it existed
    AuthData initialAuthData = JobUtils.getInitialAuthData(job, isExport);

    // Generate and store auth data
    AuthData authData = generator.generateAuthData(authResponse.getCode(), token, initialAuthData, null);
    Preconditions.checkNotNull(authData, "Auth data should not be null");

    // Update the job
    PortabilityJob updatedJob = JobUtils.setAuthData(job, authData, isExport);
    jobManager.updateJob(updatedJob);

    if(isExport) {
      // TODO: Send to auth intermediary page
      response.sendRedirect(Config.BASE_URL + "/next");  // TODO: parameterize
    } else {
      response.sendRedirect(Config.BASE_URL + "/copy");
    }

  }

  /* Return an AuthorizationCodeResponseUrl for the Oauth2 response. */
  private static AuthorizationCodeResponseUrl getResponseUrl(HttpServletRequest request) {
    StringBuffer fullUrlBuf = request.getRequestURL();
    if (request.getQueryString() != null) {
      fullUrlBuf.append('?').append(request.getQueryString());
    }
    return new AuthorizationCodeResponseUrl(fullUrlBuf.toString());
  }

  /** Looks up job and does checks that it exists. */
  private PortabilityJob lookupJob(String token) {
    PortabilityJob job = jobManager.findExistingJob(token);
    Preconditions.checkState(null != job, "existingJob not found for token: %s", token);
    return job;
  }
}
