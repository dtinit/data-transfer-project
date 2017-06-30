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
public class SelectServiceController {
  @Autowired
  private ServiceProviderRegistry serviceProviderRegistry;
  @Autowired
  private JobManager jobManager;

  /** Sets the selected service for import or export and kicks off the auth flow. */
  @CrossOrigin(origins = "http://localhost:3000")
  @RequestMapping("/_/selectService")
  @ResponseBody
  public String selectService(HttpServletRequest request,
      @RequestParam(value = "serviceName", required = true) String serviceNameParam,
      @RequestParam(value = "isExport", required = true) Boolean isExportServiceParam,
      @CookieValue(value = "jobToken", required = true) String token) throws Exception {

    // TODO: move to interceptor to redirect
    Preconditions.checkArgument(!Strings.isNullOrEmpty(token), "Token required");

    // Valid job must be present
    PortabilityJob job = jobManager.findExistingJob(token);
    Preconditions.checkState(null != job, "existingJob not found for token: %s", token);

    System.out.println("SelectServiceController: incoming, service: " + serviceNameParam + ", isExport: " + isExportServiceParam);
    System.out.println("SelectServiceController, existing job:\n\n" + job + "\n\n*****\n");

    // Valid data type is required to be set in the job by this point
    PortableDataType dataType = getDataType(job.dataType());

    String serviceName = null;
    if (isValidService(serviceNameParam, isExportServiceParam)) {
      serviceName = serviceNameParam;
      System.out.println("SelectServiceController: handling params, service: " + serviceNameParam + ", isExport: " + isExportServiceParam);
      // Process the param provided
      PortabilityJob updatedJob = setService(job, serviceName, isExportServiceParam);
      System.out.println("\n\n*****\nSelectServiceController, updatedJob:\n\n" + updatedJob + "\n\n*****\n");

      jobManager.updateJob(updatedJob);
    } else {
      System.out.println("SelectServiceController: handling persisted, service: " + serviceName + ", isExport: " + isExportServiceParam);
      // If no valid param, attempt to continue with persisted service name
      serviceName = getPersistedServiceName(job, isExportServiceParam);
      Preconditions.checkState(isValidService(serviceName, isExportServiceParam));
    }

    // Obtain the ServiceProvider from the registry
    OnlineAuthDataGenerator generator = serviceProviderRegistry.getOnlineAuth(serviceName, dataType);

    Preconditions.checkState(!Strings.isNullOrEmpty(job.token()), "Job token not set");
    String authUrl = generator.generateAuthUrl(job.token()).url();
    if (!authUrl.startsWith("https://") && ! authUrl.startsWith("http://")) {
      authUrl = "http://" + authUrl;
    }
    // Send the url for the client to redirect to service authorization
    System.out.println("\n\nRedirecting to: " + authUrl +"\n\n");
    return authUrl;
  }

  // Sets the service in the correct field of the PortabilityJob
  private PortabilityJob setService(PortabilityJob job, String serviceName, boolean isExportService) {
    System.out.println("SelectServiceController: setting service: " + serviceName + ", isExport: " + isExportService);
    PortabilityJob.Builder updatedJob = job.toBuilder();
    if (isExportService) {
      updatedJob.setExportService(serviceName);
    } else {
      updatedJob.setImportService(serviceName);
    }
    return updatedJob.build();
  }

  private static String getPersistedServiceName(PortabilityJob job, boolean isExport) {
    // Data type not provided in param, attempt to lookup the data type from storage
    return isExport ? job.exportService() : job.importService();
  }

  /** Determines whether the current service is a valid service */
  private static boolean isValidService(String serviceName, boolean isExport) {
    if(!Strings.isNullOrEmpty(serviceName)) {
      // TODO: Use service registry to validate the serice is valid for import or export
      return true;
    }
    return false;
  }

  /** Parse the data type .*/
  private static PortableDataType getDataType(String dataType) {
    Optional<PortableDataType> dataTypeOption = Enums.getIfPresent(PortableDataType.class, dataType);
    Preconditions.checkState(dataTypeOption.isPresent(), "Data type required");
    return dataTypeOption.get();
  }
}
