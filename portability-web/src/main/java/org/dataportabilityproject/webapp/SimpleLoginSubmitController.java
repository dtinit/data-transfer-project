package org.dataportabilityproject.webapp;

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
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller to process the submission of simple login credentials
 */
@RestController
public class SimpleLoginSubmitController {
  @Autowired
  private ServiceProviderRegistry serviceProviderRegistry;
  @Autowired
  private JobManager jobManager;

  /**
   * Sets the selected service for import or export and kicks off the auth flow.
   */
  @RequestMapping(path="/simpleLoginSubmit", method = RequestMethod.POST)
  public void simpleLoginSubmit(HttpServletRequest request, HttpServletResponse response,
      @CookieValue(value = "jobToken", required = true) String token) throws Exception {

    // Valid job must be present
    PortabilityJob job = jobManager.findExistingJob(token);
    Preconditions.checkState(null != job, "existingJob not found for token: %s", token);

    LogUtils.log("simpleLoginSubmit, job: %s", job);

    PortableDataType dataType = JobUtils.getDataType(job.dataType());
    LogUtils.log("dataType: %s", dataType);

    // TODO: Determine import vs export mode
    // Hack! For now, if we don't have export auth data, assume it's for export.
    boolean isExport = (null == job.exportAuthData());

    // TODO: Determine service from job or from authUrl path?
    String service = isExport ? job.exportService() : job.importService();
    Preconditions.checkState(!Strings.isNullOrEmpty(service), "service not found, service: %s isExport: %b, token: %s", service, isExport, token);
    LogUtils.log("service: %s, isExport: %b", service, isExport);


    // Username
    String username = getParam(request, "username");
    Preconditions.checkArgument(!Strings.isNullOrEmpty(username), "Missing valid username: %s", username);
    // Password
    String password = getParam(request, "password");
    Preconditions.checkArgument(!Strings.isNullOrEmpty(password), "Password is empty");

    // TODO: Validate job before going further

    // Obtain the OnlineAuthDataGenerator
    OnlineAuthDataGenerator generator = serviceProviderRegistry
        .getOnlineAuth(job.exportService(), dataType);
    Preconditions.checkNotNull(generator,"Generator not found for type: %s, service: %s",
        dataType, job.exportService());

    // Generate and store auth data
    AuthData authData = generator.generateAuthData(username, token, null, password);
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

  /** Looks up job and does checks that it exists. */
  private PortabilityJob lookupJob(String token) {
    PortabilityJob job = jobManager.findExistingJob(token);
    Preconditions.checkState(null != job, "existingJob not found for token: %s", token);
    return job;
  }

  // TODO: Determine how to get client to submit 'clean' values
  // Hack to strip angular indexing in option values
  private static String getParam(HttpServletRequest request, String name) {
    String value = request.getParameterValues(name)[0];
    String trimmed = value.substring(value.indexOf(":") + 1).trim();
    LogUtils.log("Converted: %s , result: %s", value, trimmed);
    return trimmed;
  }
}
