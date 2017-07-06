package org.dataportabilityproject.webapp;

import com.google.common.base.Enums;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import org.dataportabilityproject.ServiceProviderRegistry;
import org.dataportabilityproject.shared.PortableDataType;
import org.dataportabilityproject.shared.auth.AuthRequest;
import org.dataportabilityproject.shared.auth.OnlineAuthDataGenerator;
import org.dataportabilityproject.webapp.job.JobManager;
import org.dataportabilityproject.webapp.job.PortabilityJob;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller to render the import authorization after the export authorization step has completed.
 */
@RestController
public class ImportSetupController {
  @Autowired
  private ServiceProviderRegistry registry;

  @Autowired
  private JobManager jobManager;

  @CrossOrigin(origins = "http://localhost:3000")
  @RequestMapping("/_/importSetup")
  public Map<String, String> importSetup(
      @CookieValue(value = "jobToken", required = true) String token) throws Exception {
    // TODO: move to interceptor to redirect
    Preconditions.checkArgument(!Strings.isNullOrEmpty(token), "Token required");

    // Valid job must be present
    PortabilityJob job = jobManager.findExistingJob(token);
    Preconditions.checkState(null != job, "existingJob not found for token: %s", token);

    LogUtils.log("importSetup, job: %s", job);

    String exportService = job.exportService();
    Preconditions.checkState(!Strings.isNullOrEmpty(job.exportService()), "Export service is invalid");
    Preconditions.checkState(job.exportAuthData() != null, "Export AuthData is required");
    String importService = job.importService();
    Preconditions.checkState(!Strings.isNullOrEmpty(job.importService()), "Import service is invalid");

    // TODO: ensure import auth data doesn't exist when this is called in the auth flow
   Preconditions.checkState(job.importAuthData() == null, "Import AuthData should not exist");

    // Obtain the OnlineAuthDataGenerator
    OnlineAuthDataGenerator generator = registry.getOnlineAuth(job.importService(), getDataType(job.dataType()));

    // Auth url
    AuthRequest authRequest = generator.generateAuthUrl(job.token());

    // Store authUrl
    if (authRequest.initialAuthData() != null) {
      PortabilityJob jobBeforeInitialData = lookupJob(token);
      PortabilityJob updatedJob = JobUtils
          .setInitialAuthData(job, authRequest.initialAuthData(), true);
      jobManager.updateJob(updatedJob);
    }

    // Send the url for the client to redirect to service authorization
    LogUtils.log("Import auth url sent to client: %s", authRequest.url());

    return ImmutableMap.<String, String>builder()
        .put(JsonKeys.DATA_TYPE, job.dataType())
        .put(JsonKeys.EXPORT_SERVICE, job.exportService())
        .put(JsonKeys.IMPORT_SERVICE, job.importService())
        .put(JsonKeys.IMPORT_AUTH_URL, authRequest.url())
        .build();
  }

  /** Looks up job and does checks that it exists. */
  private PortabilityJob lookupJob(String token) {
    PortabilityJob job = jobManager.findExistingJob(token);
    Preconditions.checkState(null != job, "existingJob not found for token: %s", token);
    return job;
  }

  /** Parse the data type .*/
  private static PortableDataType getDataType(String dataType) {
    com.google.common.base.Optional<PortableDataType> dataTypeOption = Enums
        .getIfPresent(PortableDataType.class, dataType);
    Preconditions.checkState(dataTypeOption.isPresent(), "Data type required");
    return dataTypeOption.get();
  }
}
