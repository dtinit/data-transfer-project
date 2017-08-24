package org.dataportabilityproject.webapp;

import com.google.common.collect.ImmutableList;
import java.util.List;
import javax.servlet.http.HttpServletResponse;
import org.dataportabilityproject.ServiceProviderRegistry;
import org.dataportabilityproject.shared.PortableDataType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Controller for the list data types service. */
@RestController
public class ListDataTypesController {
  @Autowired
  private ServiceProviderRegistry serviceProviderRegistry;

  /** Returns of the list of data types allowed for inmport and export. */
  @RequestMapping("/_/listDataTypes")
  public List<PortableDataType> listDataTypes(@CookieValue(value = "jobToken", required = false) String token,
      HttpServletResponse response) throws Exception {

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
