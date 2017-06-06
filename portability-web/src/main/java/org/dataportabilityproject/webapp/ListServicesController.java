package org.dataportabilityproject.webapp;

import com.google.common.base.Enums;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.dataportabilityproject.ServiceProviderRegistry;
import org.dataportabilityproject.shared.PortableDataType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Controller for the list services available for export and import. */
@RestController
public class ListServicesController {
  private volatile ServiceProviderRegistry registry = null; // TODO: Consider Dependecy Injection

  @Autowired
  private ServiceProviderRegistry serviceProviderRegistry;

  /** Returns of the list of data types allowed for inmport and export. */
  @CrossOrigin(origins = "http://localhost:3000")
  @RequestMapping("/_/listServices")
  public Map<String, List<String>> listServicesForExport(@RequestParam("dataType") String type) throws Exception {
    Optional<PortableDataType> dataType = Enums.getIfPresent(PortableDataType.class, type);
    Preconditions.checkArgument(dataType.isPresent());
    List<String> exportServices = serviceProviderRegistry.getServiceProvidersThatCanExport(dataType.get());
    List<String> importServices = serviceProviderRegistry.getServiceProvidersThatCanImport(dataType.get());
    if (exportServices.isEmpty() || importServices.isEmpty()) {
      // TODO: log a warning
    }
    return ImmutableMap.<String, List<String>>of("export", exportServices, "import", importServices);
  }
}
