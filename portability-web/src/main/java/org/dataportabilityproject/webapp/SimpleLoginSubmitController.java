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

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.dataportabilityproject.ServiceProviderRegistry;
import org.dataportabilityproject.job.JobManager;
import org.dataportabilityproject.job.PortabilityJob;
import org.dataportabilityproject.shared.LogUtils;
import org.dataportabilityproject.shared.PortableDataType;
import org.dataportabilityproject.shared.Secrets;
import org.dataportabilityproject.shared.auth.AuthData;
import org.dataportabilityproject.shared.auth.OnlineAuthDataGenerator;
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
  @Autowired
  private CryptoHelper cryptoHelper;

  /**
   * Sets the selected service for import or export and kicks off the auth flow.
   */
  @RequestMapping(path="/simpleLoginSubmit", method = RequestMethod.POST)
  public void simpleLoginSubmit(HttpServletRequest request, HttpServletResponse response,
      @CookieValue(value = JsonKeys.ID_COOKIE_KEY, required = true) String encodedIdCookie) throws Exception {

    // Valid job must be present
    Preconditions.checkArgument(!Strings.isNullOrEmpty(encodedIdCookie), "Encoded Id Cookie required");
    String jobId = JobUtils.decodeId(encodedIdCookie);
    PortabilityJob job = jobManager.findExistingJob(jobId);
    Preconditions.checkState(null != job, "existingJob not found for job id: %s", jobId);

    LogUtils.log("simpleLoginSubmit, job: %s", job);

    PortableDataType dataType = JobUtils.getDataType(job.dataType());
    LogUtils.log("dataType: %s", dataType);

    // TODO: Determine import vs export mode
    // Hack! For now, if we don't have export auth data, assume it's for export.
    boolean isExport = (null == job.exportAuthData());

    // TODO: Determine service from job or from authUrl path?
    String service = isExport ? job.exportService() : job.importService();
    Preconditions.checkState(!Strings.isNullOrEmpty(service), "service not found, service: %s isExport: %b, job id: %s", service, isExport, jobId);
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
    AuthData authData = generator.generateAuthData(username, jobId, null, password);
    Preconditions.checkNotNull(authData, "Auth data should not be null");

    // Update the job
    // TODO: Remove persistence of auth data in storage at this point
    PortabilityJob updatedJob = JobUtils.setAuthData(job, authData, isExport);
    jobManager.updateJob(updatedJob);

    // Set new cookie
    cryptoHelper.encryptAndSetCookie(response, isExport, authData);

    if(isExport) {
      // TODO: Send to auth intermediary page
      response.sendRedirect(Secrets.getInstance().baseUrl() + "/next");  // TODO: parameterize
    } else {
      response.sendRedirect(Secrets.getInstance().baseUrl() + "/copy");
    }

  }

  /** Looks up job and does checks that it exists. */
  private PortabilityJob lookupJob(String id) {
    PortabilityJob job = jobManager.findExistingJob(id);
    Preconditions.checkState(null != job, "existingJob not found for id: %s", id);
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
