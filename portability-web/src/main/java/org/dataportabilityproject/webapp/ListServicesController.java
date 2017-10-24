/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.dataportabilityproject.webapp;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.dataportabilityproject.ServiceProviderRegistry;
import org.dataportabilityproject.shared.PortableDataType;
import org.dataportabilityproject.job.JobManager;
import org.springframework.beans.factory.annotation.Autowired;
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
