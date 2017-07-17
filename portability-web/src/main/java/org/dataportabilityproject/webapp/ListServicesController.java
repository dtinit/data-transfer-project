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
import org.springframework.beans.factory.annotation.Autowired;
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
      HttpServletResponse response) throws Exception {

    Preconditions.checkArgument(!Strings.isNullOrEmpty(dataTypeParam), "Missing data type");
    LogUtils.log("ListServicesController: using data type param: %s",  dataTypeParam);

    // Validate incoming data type parameter
    PortableDataType dataType = JobUtils.getDataType(dataTypeParam);

    // Return services for the given data type
    List<String> exportServices = serviceProviderRegistry.getServiceProvidersThatCanExport(dataType);
    List<String> importServices = serviceProviderRegistry.getServiceProvidersThatCanImport(dataType);
    if (exportServices.isEmpty() || importServices.isEmpty()) {
      LogUtils.log("Empty service list found, export size: %d, import size: %d", exportServices.size(), importServices.size());
    }
    return ImmutableMap.<String, List<String>>of(JsonKeys.EXPORT, exportServices, JsonKeys.IMPORT, importServices);
  }
}
