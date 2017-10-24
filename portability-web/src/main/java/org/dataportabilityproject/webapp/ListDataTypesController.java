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

  /** Returns of the list of data types allowed for import and export. */
  @RequestMapping("/_/listDataTypes")
  public List<PortableDataType> listDataTypes(
      @CookieValue(value = JsonKeys.ID_COOKIE_KEY, required = false) String encodedIdCookie,
      HttpServletResponse response) throws Exception {

    // TODO: Determine what to do if an encodedIdCookie exists

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
