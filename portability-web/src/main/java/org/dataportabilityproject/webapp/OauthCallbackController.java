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
import org.dataportabilityproject.PortabilityFlags;
import org.dataportabilityproject.ServiceProviderRegistry;
import org.dataportabilityproject.job.JobDao;
import org.dataportabilityproject.job.JobUtils;
import org.dataportabilityproject.job.PortabilityJob;
import org.dataportabilityproject.shared.LogUtils;
import org.dataportabilityproject.shared.PortableDataType;
import org.dataportabilityproject.shared.auth.AuthData;
import org.dataportabilityproject.shared.auth.OnlineAuthDataGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Controller for the list data types service. */
@RestController
public class OauthCallbackController {
  @Autowired
  private ServiceProviderRegistry serviceProviderRegistry;
  @Autowired
  private JobDao jobDao;
  @Autowired
  private CryptoHelper cryptoHelper;

  /** Handle oauth callback requests. */
  @RequestMapping("/callback1/**")
  public void handleOauthResponse(
      @CookieValue(value = JsonKeys.ID_COOKIE_KEY, required = true) String encodedIdCookie,
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
    Preconditions.checkArgument(!Strings.isNullOrEmpty(encodedIdCookie), "Encoded Id cookie required");
    String jobId = JobUtils.decodeId(encodedIdCookie);

    PortabilityJob job = lookupJob(jobId);

    PortableDataType dataType = JobUtils.getDataType(job.dataType());
    LogUtils.log("dataType: %s", dataType);

    // TODO: Support import and export service
    // Hack! For now, if we don't have export auth data, assume it's for export.
    boolean isExport = (null == job.exportAuthData());

    // TODO: Determine service from job or from authUrl path?
    String service = isExport ? job.exportService() : job.importService();
    Preconditions.checkState(!Strings.isNullOrEmpty(service), "service not found, service: %s isExport: %b, job id: %s", service, isExport, jobId);
    LogUtils.log("service: %s, isExport: %b", service, isExport);

    // Obtain the ServiceProvider from the registry
    OnlineAuthDataGenerator generator = serviceProviderRegistry.getOnlineAuth(service, dataType);

    // Retrieve initial auth data, if it existed
    AuthData initialAuthData = JobUtils.getInitialAuthData(job, isExport);
    Preconditions.checkNotNull(initialAuthData, "Initial AuthData expected during Oauth 1.0 flow");

    // Generate and store auth data
    AuthData authData = generator
        .generateAuthData(PortabilityServerFlags.baseApiUrl(), oauthVerifier, jobId,
            initialAuthData, null);

    // Update the job
    // TODO: Remove persistence of auth data in storage at this point. The data will be passed
    // thru to the client via the cookie.
    PortabilityJob updatedJob = JobUtils.setAuthData(job, authData, isExport);
    jobDao.updateJob(updatedJob);

    // Set new cookie
    cryptoHelper.encryptAndSetCookie(response, isExport, authData);

    if(isExport) {
      // TODO: Send to auth intermediary page
      response.sendRedirect(PortabilityServerFlags.baseUrl() + "/next");  // TODO: parameterize
    } else {
      response.sendRedirect(PortabilityServerFlags.baseUrl() + "/copy");
    }
  }

  /** Looks up job and does checks that it exists. */
  private PortabilityJob lookupJob(String token) {
    PortabilityJob job = jobDao.findExistingJob(token);
    Preconditions.checkState(null != job, "existingJob not found for token: %s", token);
    return job;
  }
}
