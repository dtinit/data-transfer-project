package org.dataportabilityproject.webapp;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.dataportabilityproject.ServiceProviderRegistry;
import org.dataportabilityproject.shared.PortableDataType;
import org.dataportabilityproject.shared.auth.AuthFlowInitiator;
import org.dataportabilityproject.shared.auth.OnlineAuthDataGenerator;
import org.dataportabilityproject.webapp.job.JobManager;
import org.dataportabilityproject.webapp.job.PortabilityJob;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller to process the configuration submitted via the form and responds with the redirect
 * for the export service.
 */
@RestController
public class ConfigureController {
  @Autowired
  private ServiceProviderRegistry serviceProviderRegistry;
  @Autowired
  private JobManager jobManager;

  /**
   * Sets the selected service for import or export and kicks off the auth flow.
   */
  @CrossOrigin(origins = "http://localhost:3000")
  @RequestMapping(path="/configure", method = RequestMethod.POST)
  public void configure(HttpServletRequest request, HttpServletResponse response,
      @CookieValue(value = "jobToken", required = false) String tokenCookie) throws Exception {

    LogUtils.log("Configure: %s", request.getRequestURI());
    for (String key: request.getParameterMap().keySet()) {
      LogUtils.log("Parameter key: %s, value: %s", key, request.getParameter(key));
    }

    // TODO: Consider what to do if previous job exists in the session
    String existingToken = null;
    PortabilityJob existingJob = null;
    if (!Strings.isNullOrEmpty(tokenCookie)) {
      existingToken = tokenCookie;
      LogUtils.log("Found existing cookie, ignoring previous values");
      existingJob = jobManager.findExistingJob(tokenCookie);
      if(existingJob != null) {
        LogUtils.log("Found existing job, ignoring previous values");
      } else {
        LogUtils.log("Found existing cookie but no job");
      }

    }

    // Either token was empty or the job it represented was not found, create a new one
    String dataTypeStr = getParam(request, "dataType");
    Preconditions.checkArgument(!Strings.isNullOrEmpty(dataTypeStr), "Missing valid dataTypeParam: %s", dataTypeStr);
    PortableDataType dataType = JobUtils.getDataType(dataTypeStr);

    String exportService = getParam(request, "exportService");
    Preconditions.checkArgument(JobUtils.isValidService(exportService, true), "Missing valid exportService: %s", exportService);

    String importService = getParam(request, "importService");
    Preconditions.checkArgument(JobUtils.isValidService(importService, false), "Missing valid importService: %s", importService);

    String token = jobManager.createNewUserjob(dataType, exportService, importService);

    // Set new cookie
    Cookie cookie = new Cookie("jobToken", token);
    LogUtils.log("Set new cookie with token: %s", token);
    response.addCookie(cookie);


    // Lookup job, even if just recently created
    PortabilityJob job = lookupJob(token);
    Preconditions.checkState(job != null, "Job required");
    Preconditions.checkState(!Strings.isNullOrEmpty(job.token()), "Job token not set");

    // TODO: Validate job before going further

    // Obtain the OnlineAuthDataGenerator
    OnlineAuthDataGenerator generator = serviceProviderRegistry
        .getOnlineAuth(job.exportService(), dataType);
    Preconditions.checkNotNull(generator,"Generator not found for type: %s, service: %s",
        dataType, job.exportService());

    // Auth authUrl
    AuthFlowInitiator authFlowInitiator = generator.generateAuthUrl(job.token());
    Preconditions.checkNotNull(authFlowInitiator,"AuthFlowInitiator not found for type: %s, service: %s",
        dataType, job.exportService());

    // Store initial auth data
    if (authFlowInitiator.initialAuthData() != null) {
      PortabilityJob jobBeforeInitialData = lookupJob(token);
      PortabilityJob updatedJob = JobUtils
          .setInitialAuthData(job, authFlowInitiator.initialAuthData(), true);
      jobManager.updateJob(updatedJob);
    }

    // Send the authUrl for the client to redirect to service authorization
    LogUtils.log("Redirecting to: %s", authFlowInitiator.authUrl());
    response.sendRedirect(authFlowInitiator.authUrl());
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
