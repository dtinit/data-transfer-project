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

import com.google.api.client.auth.oauth2.AuthorizationCodeResponseUrl;
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
import org.springframework.web.bind.annotation.RestController;

/** Controller for the list data types service. */
@RestController
public class Oauth2CallbackController {
  @Autowired
  private ServiceProviderRegistry serviceProviderRegistry;
  @Autowired
  private JobManager jobManager;
  @Autowired
  private CryptoHelper cryptoHelper;

  /** Handle Oauth2 callback requests. */
  @RequestMapping("/callback/**")
  public void handleOauth2Response(
      @CookieValue(value = JsonKeys.ID_COOKIE_KEY, required = true) String encodedIdCookie,
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

    Preconditions.checkArgument(!Strings.isNullOrEmpty(encodedIdCookie), "Encoded Id cookie required");
    String jobId = JobUtils.decodeId(encodedIdCookie);

    // State token includes only ID for now. Might want to add more info.
    String state = JobUtils.decodeId(authResponse.getState());

    // TODO: Remove sanity check
    Preconditions.checkState(state.equals(jobId), "Job id in cookie [%s] and request [%s] should match", jobId, state);

    PortabilityJob job = lookupJob(jobId);
    PortableDataType dataType = JobUtils.getDataType(job.dataType());
    LogUtils.log("dataType: %s", dataType);

    // TODO: Determine import vs export mode
    // Hack! For now, if we don't have export auth data, assume it's for export.
    boolean isExport = (null == job.exportAuthData());

    // TODO: Determine service from job or from authUrl path?
    String service = isExport ? job.exportService() : job.importService();
    Preconditions.checkState(!Strings.isNullOrEmpty(service),
        "service not found, service: %s isExport: %b, jobId: %s", service, isExport, jobId);
    LogUtils.log("service: %s, isExport: %b", service, isExport);

    // Obtain the ServiceProvider from the registry
    OnlineAuthDataGenerator generator = serviceProviderRegistry.getOnlineAuth(service, dataType);

    // Retrieve initial auth data, if it existed
    AuthData initialAuthData = JobUtils.getInitialAuthData(job, isExport);

    // Generate and store auth data
    AuthData authData = generator.generateAuthData(authResponse.getCode(), jobId, initialAuthData, null);
    Preconditions.checkNotNull(authData, "Auth data should not be null");

    // Update the job
    // TODO: Remove persistence of auth data in storage at this point. The data will be passed
    // thru to the client via the cookie.
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


  /* Return an AuthorizationCodeResponseUrl for the Oauth2 response. */
  private static AuthorizationCodeResponseUrl getResponseUrl(HttpServletRequest request) {
    StringBuffer fullUrlBuf = request.getRequestURL();
    if (request.getQueryString() != null) {
      fullUrlBuf.append('?').append(request.getQueryString());
    }
    return new AuthorizationCodeResponseUrl(fullUrlBuf.toString());
  }

  /** Looks up job and does checks that it exists. */
  private PortabilityJob lookupJob(String id) {
    PortabilityJob job = jobManager.findExistingJob(id);
    Preconditions.checkState(null != job, "existingJob not found for job id: %s", id);
    return job;
  }
}
