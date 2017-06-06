package org.dataportabilityproject.webapp;

import java.io.IOException;
import org.dataportabilityproject.ServiceProviderRegistry;
import com.google.common.collect.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.dataportabilityproject.shared.PortableDataType;
import org.dataportabilityproject.shared.Secrets;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CrossOrigin;

/** Controller for the list data types service. */
@RestController
public class ListDataTypesController {
  private final AtomicLong counter = new AtomicLong();
  private volatile ServiceProviderRegistry registry = null; // TODO: Consider Dependecy Injection

  @Autowired
  private ServiceProviderRegistry serviceProviderRegistry;

  /** Returns of the list of data types allowed for inmport and export. */
  @CrossOrigin(origins = "http://localhost:3000")
  @RequestMapping("/_/listDataTypes")
  public List<PortableDataType> listDataTypes() throws Exception {
    ImmutableList.Builder<PortableDataType> types = ImmutableList.builder();
    for (PortableDataType type : PortableDataType.values()) {
      if (hasImportAndExport(type)) {
        types.add(type);
      }
    }
    return types.build();
  }

  /**
   * Returns whether or not the given {@link PortableDataType} has at least one registered service
   * that can import and at least one registered service that can export.
   */
  private boolean hasImportAndExport(PortableDataType type) throws Exception {
    return
        (serviceProviderRegistry.getServiceProvidersThatCanExport(type).size() > 0)
            &&
            (serviceProviderRegistry.getServiceProvidersThatCanImport(type).size() > 0);
  }
}
