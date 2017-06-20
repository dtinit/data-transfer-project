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
import org.dataportabilityproject.webapp.job.JobManager;
import org.dataportabilityproject.webapp.job.PortabilityJob;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/** Controller for the list services available for export and import. */
@RestController
public class ListServicesController {
  @Autowired
  private ServiceProviderRegistry serviceProviderRegistry;
  @Autowired
  private JobManager jobManager;

  /** Returns of the list of data types allowed for inmport and export. */
  @CrossOrigin(origins = "http://localhost:3000")
  @RequestMapping("/_/listServices")
  @ResponseBody
  public Map<String, List<String>> listServices(HttpServletRequest request,
      @RequestParam(value = "dataType", required = false) final String dataTypeParam,
      @CookieValue(value = "jobToken", required = true) final String token,
      HttpServletResponse response) throws Exception {

    // TODO: move to interceptor to redirect
    Preconditions.checkArgument(!Strings.isNullOrEmpty(token), "Token required");

    // Valid job must be present
    PortabilityJob job = jobManager.findExistingJob(token);
    Preconditions.checkState(null != job, "existingJob not found for token: %s", token);

    PortableDataType dataType = null;
    if(!Strings.isNullOrEmpty(dataTypeParam)) {
      // Process and persist the incoming data type parameter
      dataType = getDataType(dataTypeParam);
      // Update the database to set this data type as the selected
      PortabilityJob updatedJob = job.toBuilder().setDataType(dataType.name()).build();
      jobManager.updateJob(updatedJob);
    } else {
      // Data type not provided in param, attempt to lookup the data type from storage
      if(!Strings.isNullOrEmpty(job.dataType())) {
        dataType = getDataType(job.dataType());
      }
    }

    // Arrived at this page without data type in param or persisted,
    Preconditions.checkNotNull(dataType, "Data type not found in param or storage");

    // Return services for the given data type
    List<String> exportServices = serviceProviderRegistry.getServiceProvidersThatCanExport(dataType);
    // TODO: Remove import if not needed
    List<String> importServices = serviceProviderRegistry.getServiceProvidersThatCanImport(dataType);
    if (exportServices.isEmpty() || importServices.isEmpty()) {
      // TODO: log a warning
    }
    return ImmutableMap.<String, List<String>>of(JsonKeys.EXPORT, exportServices, JsonKeys.IMPORT, importServices);
  }

  /** Parse the data type .*/
  private static PortableDataType getDataType(String dataType) {
    Optional<PortableDataType> dataTypeOption = Enums.getIfPresent(PortableDataType.class, dataType);
    Preconditions.checkArgument(dataTypeOption.isPresent(), "Data type required");
    return dataTypeOption.get();
  }
}
