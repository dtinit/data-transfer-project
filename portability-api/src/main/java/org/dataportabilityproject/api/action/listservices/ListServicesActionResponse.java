/*
 * Copyright 2018 The Data Transfer Project Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.dataportabilityproject.api.action.listservices;

import java.util.Set;

/** The result of a request to list importServices available for export and import. */
public class ListServicesActionResponse {

  private final Set<String> importServices;
  private final Set<String> exportServices;
  private final String errorMsg;

  private ListServicesActionResponse(
      Set<String> importServices, Set<String> exportServices, String errorMsg) {
    this.importServices = importServices;
    this.exportServices = exportServices;
    this.errorMsg = errorMsg;
  }

  public static final ListServicesActionResponse create(
      Set<String> importServices, Set<String> exportServices) {
    return new ListServicesActionResponse(importServices, exportServices, null);
  }

  public static final ListServicesActionResponse createWithError(String errorMsg) {
    return new ListServicesActionResponse(null, null, errorMsg);
  }

  public Set<String> getImportServices() {
    return importServices;
  }

  public String getErrorMsg() {
    return errorMsg;
  }

  public Set<String> getExportServices() {
    return exportServices;
  }
}
