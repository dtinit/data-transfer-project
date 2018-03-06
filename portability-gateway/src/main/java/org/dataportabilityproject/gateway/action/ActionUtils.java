/*
 * Copyright 2018 The Data-Portability Project Authors.
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
package org.dataportabilityproject.gateway.action;

import com.google.common.base.Strings;

/** Helper functions for validating action related data. */
public final class ActionUtils {

  /** Determines whether the current service is a valid service for export. */
  public static boolean isValidExportService(String serviceName) {
    if (!Strings.isNullOrEmpty(serviceName)) {
      // TODO: Use service registry to validate the service is valid for import or export
      return true;
    }
    return false;
  }

  /** Determines whether the current service is a valid service for import. */
  public static boolean isValidImportService(String serviceName) {
    if (!Strings.isNullOrEmpty(serviceName)) {
      // TODO: Use service registry to validate the service is valid for import or export
      return true;
    }
    return false;
  }

  /** Determines whether the current service is a valid service for import. */
  public static boolean isValidTransferDataType(String transferDataType) {
    if (!Strings.isNullOrEmpty(transferDataType)) {
      // TODO: Use service registry to validate the transferDataType is valid
      return true;
    }
    return false;
  }
}
