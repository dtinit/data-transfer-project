package org.dataportabilityproject.webapp;

import com.google.common.base.Enums;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.dataportabilityproject.ServiceProviderRegistry;
import org.dataportabilityproject.shared.PortableDataType;
import org.dataportabilityproject.shared.ServiceProvider;
import org.dataportabilityproject.shared.auth.OfflineAuthDataGenerator;
import org.dataportabilityproject.shared.auth.OnlineAuthDataGenerator;
import org.dataportabilityproject.webapp.job.JobManager;
import org.dataportabilityproject.webapp.job.PortabilityJob;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/** Controller to process the selection of a service to export from. */
@RestController
public class SelectExportServiceController {
  @Autowired
  private ServiceProviderRegistry serviceProviderRegistry;
  @Autowired
  private JobManager jobManager;

  /** Returns of the list of data types allowed for inmport and export. */
  @CrossOrigin(origins = "http://localhost:3000")
  @RequestMapping("/_/selectExportService")
  @ResponseBody
  public String selectExportService(HttpServletRequest request,
      @RequestParam(value = "exportService", required = false) String exportServiceParam,
      @CookieValue(value = "jobToken", required = true) String token) throws Exception {

    // TODO: move to interceptor to redirect
    Preconditions.checkArgument(!Strings.isNullOrEmpty(token), "Token required");

    // Valid job must be present
    PortabilityJob job = jobManager.findExistingJob(token);
    Preconditions.checkState(null != job, "existingJob not found for token: %s", token);

    // Valid data type is required to be set in the job by this point
    PortableDataType dataType = getDataType(job.dataType());

    String selectedExportService = null;
    if(!Strings.isNullOrEmpty(exportServiceParam)) {
      // Process and persist the incoming export service parameter
      selectedExportService = isValidService(exportServiceParam) ? exportServiceParam : null;
      // Update the database to set this export service as the selected
      PortabilityJob updatedJob = job.toBuilder().setExportService(selectedExportService).build();
      jobManager.updateJob(updatedJob);
    } else {
      // Data type not provided in param, attempt to lookup the data type from storage
      if(!Strings.isNullOrEmpty(job.exportService())) {
        selectedExportService = isValidService(job.exportService()) ? job.exportService() : null;
      }
    }

    // Arrived at this page without data type in param or persisted,
    Preconditions.checkNotNull(selectedExportService, "Export service not found in param or storage");

    // Obtain the ServiceProvider from the registry
    OnlineAuthDataGenerator generator = serviceProviderRegistry.getOnlineAuth(selectedExportService, dataType);


    Preconditions.checkState(!Strings.isNullOrEmpty(job.token()), "Job token not set");
    String authUrl = generator.generateAuthUrl(job.token());
    if (!authUrl.startsWith("https://") && ! authUrl.startsWith("http://")) {
      authUrl = "http://" + authUrl;
    }
    // Send the url for the client to redirect to service authorization
    System.out.println("\n\nRedirecting to: " + authUrl +"\n\n");
    return authUrl;
  }

  /** Determines whether the current service is a valid export service */
  private static boolean isValidService(String exportService) {
    // TODO: Use service registry to math
    return true;
  }


  /** Parse the data type .*/
  private static PortableDataType getDataType(String dataType) {
    Optional<PortableDataType> dataTypeOption = Enums.getIfPresent(PortableDataType.class, dataType);
    Preconditions.checkState(dataTypeOption.isPresent(), "Data type required");
    return dataTypeOption.get();
  }
}
