package org.dataportabilityproject.webapp;

import com.google.api.client.auth.oauth2.AuthorizationCodeResponseUrl;
import com.google.common.base.Enums;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.dataportabilityproject.ServiceProviderRegistry;
import org.dataportabilityproject.shared.PortableDataType;
import org.dataportabilityproject.shared.auth.AuthData;
import org.dataportabilityproject.shared.auth.OnlineAuthDataGenerator;
import org.dataportabilityproject.webapp.job.JobManager;
import org.dataportabilityproject.webapp.job.PortabilityJob;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Controller for the list data types service. */
@RestController
public class OauthCallbackController {
  @Autowired
  private ServiceProviderRegistry serviceProviderRegistry;
  @Autowired
  private JobManager jobManager;

  /** Handle oauth callback requests. */
  @CrossOrigin(origins = "http://localhost:3000")
  @RequestMapping("/callback1/**")
  public void handleOauthResponse(
      @CookieValue(value = "jobToken", required = true) String token,
      HttpServletRequest request,
      HttpServletResponse response) throws Exception {
    LogUtils.log("OauthCallbackController getRequestURI: %s", request.getRequestURI());
    LogUtils.log("OauthCallbackController getQueryString: %s", request.getQueryString());

    String oauthToken = request.getParameterMap().get("oauth_token")[0];
    LogUtils.log("oauthToken: %s", oauthToken);
    String oauthVerifier = request.getParameterMap().get("oauth_verifier")[0];
    LogUtils.log("oauthVerifier: %s", oauthVerifier);

    Preconditions.checkArgument(!Strings.isNullOrEmpty(oauthToken), "Missing oauth_token");
    Preconditions.checkArgument(!Strings.isNullOrEmpty(oauthVerifier), "Missing oauth_verifier");

    // Valid job must be present
    PortabilityJob job = lookupJob(token);

    PortableDataType dataType = JobUtils.getDataType(job.dataType());
    LogUtils.log("dataType: %s", dataType);

    // TODO: Support import and export service
    // Hack! For now, if we don't have export auth data, assume it's for export.
    boolean isExport = (null == job.exportAuthData());

    // TODO: Determine service from job or from url path?
    String service = isExport ? job.exportService() : job.importService();
    Preconditions.checkState(!Strings.isNullOrEmpty(service), "service not found, service: %s isExport: %b, token: %s", service, isExport, token);
    LogUtils.log("service: %s, isExport: %b", service, isExport);

    // Obtain the ServiceProvider from the registry
    OnlineAuthDataGenerator generator = serviceProviderRegistry.getOnlineAuth(service, dataType);

    // Retrieve initial auth data, if it existed
    AuthData initialAuthData = JobUtils.getInitialAuthData(job, isExport);
    Preconditions.checkNotNull(initialAuthData, "Initial AuthData expected during Oauth 1.0 flow");

    // Generate and store auth data
    AuthData authData = generator.generateAuthData(oauthVerifier, token, initialAuthData);

    // Update the job
    PortabilityJob updatedJob = JobUtils.setAuthData(job, authData, isExport);
    jobManager.updateJob(updatedJob);

    if(isExport) {
      // TODO: Send to auth intermediary page
      response.sendRedirect("http://localhost:3000/next");  // TODO: parameterize
    } else {
      response.sendRedirect("http://localhost:3000/copy");
    }

  }

  /** Looks up job and does checks that it exists. */
  private PortabilityJob lookupJob(String token) {
    PortabilityJob job = jobManager.findExistingJob(token);
    Preconditions.checkState(null != job, "existingJob not found for token: %s", token);
    return job;
  }
}
