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

  /** Returns of the list of data types allowed for inmport and export. */
  @CrossOrigin(origins = "http://localhost:3000")
  @RequestMapping("/callback/.*")
  public void handleOauthResponse(HttpServletRequest request,
      HttpServletResponse response) throws Exception {

    AuthorizationCodeResponseUrl authResponse = getResponseUrl(request);

    // check for user-denied error
    if (authResponse.getError() != null) {
      System.out.println("Authorization DENIED: " + authResponse.getError());
      response.sendRedirect("/error");
      return;
    }

    // TODO: Encrypt/decrypt state param with secure info
    String token = authResponse.getState();
    Preconditions.checkArgument(!Strings.isNullOrEmpty(token), "Token required");
    log("token: %s", token);

    // Valid job must be present
    PortabilityJob job = jobManager.findExistingJob(token);
    Preconditions.checkState(null != job, "existingJob not found for token: %s", token);
    log("job: %s", job);

    PortableDataType dataType = getDataType(job.dataType());
    log("dataType: %s", dataType);

    // TODO: Determine export service from job or from url path?
    String exportService = job.exportService();
    Preconditions.checkState(!Strings.isNullOrEmpty(exportService), "exportService not found for token: %s", token);
    log("exportService: %s", exportService);

    // Obtain the ServiceProvider from the registry
    OnlineAuthDataGenerator generator = serviceProviderRegistry.getOnlineAuth(exportService, dataType);

    // Generate and store auth data
    AuthData authData = generator.generateAuthData(authResponse.getCode(), token);

    // Update the job
    PortabilityJob updatedJob = job.toBuilder().setExportAuthData(authData).build();
    jobManager.updateJob(updatedJob);

    response.sendRedirect("http://localhost:3000/import");

  }

  private static AuthorizationCodeResponseUrl getResponseUrl(HttpServletRequest request) {
    StringBuffer fullUrlBuf = request.getRequestURL();
    if (request.getQueryString() != null) {
      fullUrlBuf.append('?').append(request.getQueryString());
    }
    return new AuthorizationCodeResponseUrl(fullUrlBuf.toString());
  }

  /** Parse the data type .*/
  private static PortableDataType getDataType(String dataType) {
    Optional<PortableDataType> dataTypeOption = Enums.getIfPresent(PortableDataType.class, dataType);
    Preconditions.checkState(dataTypeOption.isPresent(), "Data type required");
    return dataTypeOption.get();
  }

  private void log (String fmt, Object... args) {
    System.out.println(String.format(fmt, args));
  }
}
