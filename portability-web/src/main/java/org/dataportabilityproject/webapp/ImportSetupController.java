package org.dataportabilityproject.webapp;

import com.google.common.base.Enums;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import java.util.Map;
import org.dataportabilityproject.ServiceProviderRegistry;
import org.dataportabilityproject.shared.PortableDataType;
import org.dataportabilityproject.shared.auth.AuthFlowInitiator;
import org.dataportabilityproject.shared.auth.OnlineAuthDataGenerator;
import org.dataportabilityproject.webapp.job.JobManager;
import org.dataportabilityproject.webapp.job.PortabilityJob;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CookieValue;
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

    // Auth authUrl
    AuthFlowInitiator authFlowInitiator = generator.generateAuthUrl(job.token());

    // Store authUrl
    if (authFlowInitiator.initialAuthData() != null) {
      PortabilityJob jobBeforeInitialData = lookupJob(token);
      PortabilityJob updatedJob = JobUtils
          .setInitialAuthData(job, authFlowInitiator.initialAuthData(), true);
      jobManager.updateJob(updatedJob);
    }

    // Send the authUrl for the client to redirect to service authorization
    LogUtils.log("Import auth authUrl sent to client: %s", authFlowInitiator.authUrl());

    return ImmutableMap.<String, String>builder()
        .put(JsonKeys.DATA_TYPE, job.dataType())
        .put(JsonKeys.EXPORT_SERVICE, job.exportService())
        .put(JsonKeys.IMPORT_SERVICE, job.importService())
        .put(JsonKeys.IMPORT_AUTH_URL, authFlowInitiator.authUrl())
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
