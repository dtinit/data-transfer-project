package org.dataportabilityproject.webapp;

import com.google.common.base.Strings;
import java.io.IOException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import org.dataportabilityproject.ServiceProviderRegistry;
import com.google.common.collect.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.dataportabilityproject.shared.PortableDataType;
import org.dataportabilityproject.shared.Secrets;
import org.dataportabilityproject.webapp.job.JobManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CrossOrigin;

/** Controller for the list data types service. */
@RestController
public class ListDataTypesController {
  @Autowired
  private ServiceProviderRegistry serviceProviderRegistry;
  @Autowired
  private JobManager jobManager;

  /** Returns of the list of data types allowed for inmport and export. */
  @CrossOrigin(origins = "http://localhost:3000")
  @RequestMapping("/_/listDataTypes")
  public List<PortableDataType> listDataTypes(@CookieValue(value = "jobToken", required = false) String token,
      HttpServletResponse response) throws Exception {
    // TODO: Move to interceptor
    System.out.println("listDataTypes, token: " + token);
    if (Strings.isNullOrEmpty(token)) {
      String newToken = jobManager.createNewUserjob();
      Cookie cookie = new Cookie("jobToken", newToken);
       response.addCookie(cookie);
       System.out.println("Added cookie: " + cookie);
    }

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
