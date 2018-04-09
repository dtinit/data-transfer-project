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
package org.dataportabilityproject.api.action.listdatatypes;

import java.util.Set;

/** The result of a request to list data types available for export and import. */
public class ListDataTypesActionResponse {

  private final Set<String> transferDataTypes;
  private final String errorMsg;

  private ListDataTypesActionResponse(Set<String> transferDataTypes, String errorMsg) {
    this.transferDataTypes = transferDataTypes;
    this.errorMsg = errorMsg;
  }

  public static final ListDataTypesActionResponse create(Set<String> dataTypes) {
    return new ListDataTypesActionResponse(dataTypes, null);
  }

  public static final ListDataTypesActionResponse createWithError(String errorMsg) {
    return new ListDataTypesActionResponse(null, errorMsg);
  }

  public Set<String> getTransferDataTypes() {
    return transferDataTypes;
  }

  public String getErrorMsg() {
    return errorMsg;
  }
}
