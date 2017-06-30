package org.dataportabilityproject.webapp;

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
import org.dataportabilityproject.webapp.job.JobManager;
import org.dataportabilityproject.webapp.job.PortabilityJob;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Fetch the configuration to initiate the copy. */
@RestController
public class FetchCopyConfigurationController {

  @Autowired
  private JobManager jobManager;

  /** Returns of the list of data types allowed for inmport and export. */
  @CrossOrigin(origins = "http://localhost:3000")
  @RequestMapping("/_/fetchCopyConfiguration")
  public Map<String, String> fetchCopyConfiguration(
      @CookieValue(value = "jobToken", required = true) String token) throws Exception {
    // TODO: move to interceptor to redirect
    Preconditions.checkArgument(!Strings.isNullOrEmpty(token), "Token required");

    // Valid job must be present
    PortabilityJob job = jobManager.findExistingJob(token);
    Preconditions.checkState(null != job, "existingJob not found for token: %s", token);

    LogUtils.log("fetchCopyConfiguration, job: %s", job);

    String exportService = job.exportService();
    Preconditions.checkState(!Strings.isNullOrEmpty(job.exportService()), "Export service is invalid");
    Preconditions.checkState(job.exportAuthData() != null, "Export AuthData is required");
    String importService = job.importService();
    Preconditions.checkState(!Strings.isNullOrEmpty(job.importService()), "Import service is invalid");

    // TODO: ensure import auth data doesn't exist when this is called in the auth flow
    //Preconditions.checkState(job.importAuthData() != null, "Import AuthData is required");

    return ImmutableMap.<String, String>builder()
        .put(JsonKeys.DATA_TYPE, job.dataType())
        .put(JsonKeys.EXPORT_SERVICE, job.exportService())
        .put(JsonKeys.EXPORT_SERVICE_AUTH_EXISTS, String.valueOf(job.exportAuthData() != null))
        .put(JsonKeys.EXPORT_AUTH_URL, "TODO: Implement")
        .put(JsonKeys.IMPORT_SERVICE, job.importService())
        .put(JsonKeys.IMPORT_SERVICE_AUTH_EXISTS, String.valueOf(job.importAuthData() != null))
        .put(JsonKeys.IMPORT_AUTH_URL, "TODO: Implement")
        .build();
  }
}
