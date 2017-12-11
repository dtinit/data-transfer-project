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
package org.dataportabilityproject.job;

import com.google.common.base.Charsets;
import com.google.common.base.Enums;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.io.BaseEncoding;
import org.dataportabilityproject.shared.LogUtils;
import org.dataportabilityproject.shared.PortableDataType;
import org.dataportabilityproject.shared.auth.AuthData;

/**
 * Utility methods for handling data in the related to jobss.
 */
public class JobUtils {

  public static String decodeId(String encoded) {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(encoded));
    return new String(BaseEncoding.base64Url().decode(encoded), Charsets.UTF_8);
  }

  public static String encodeId(PortabilityJob job) {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(job.id()));
    return BaseEncoding.base64Url().encode(job.id().getBytes(Charsets.UTF_8));
  }

  /* Returns the initial auth data for export or import determined by the {@code isExport} param. */
  public static String getService(PortabilityJob job, boolean isExport) {
    String service = isExport ? job.exportService() : job.importService();
    Preconditions
        .checkState(!Strings.isNullOrEmpty(service), "service not found, service: %s isExport: %b",
            service, isExport);
    return service;
  }

  /* Returns the initial auth data for export or import determined by the {@code isExport} param. */
  public static AuthData getInitialAuthData(PortabilityJob job, boolean isExport) {
    return isExport ? job.exportInitialAuthData() : job.importInitialAuthData();
  }

  /* Sets the service in the correct field of the PortabilityJob */
  public static PortabilityJob setAuthData(PortabilityJob job, AuthData authData,
      boolean isExportService) {
    PortabilityJob.Builder updatedJob = job.toBuilder();
    if (isExportService) {
      updatedJob.setExportAuthData(authData);
    } else {
      updatedJob.setImportAuthData(authData);
    }
    return updatedJob.build();
  }

  /* Sets the service in the correct field of the PortabilityJob */
  public static PortabilityJob setService(PortabilityJob job, String serviceName,
      boolean isExport) {
    LogUtils.log("Setting service: %s, isExport:  %s", serviceName, isExport);
    PortabilityJob.Builder updatedJob = job.toBuilder();
    if (isExport) {
      updatedJob.setExportService(serviceName);
    } else {
      updatedJob.setImportService(serviceName);
    }
    return updatedJob.build();
  }

  /* Sets the service in the correct field of the PortabilityJob */
  public static PortabilityJob setInitialAuthData(PortabilityJob job, AuthData initialAuthData,
      boolean isExport) {
    LogUtils.log("Setting initialAuthData: %s, isExport:  %s", initialAuthData, isExport);
    PortabilityJob.Builder updatedJob = job.toBuilder();
    if (isExport) {
      updatedJob.setExportInitialAuthData(initialAuthData);
    } else {
      updatedJob.setImportInitialAuthData(initialAuthData);
    }
    return updatedJob.build();
  }


  /* Gets the export or import service name, depending on the given {@code isExport} param. */
  public static String getServiceName(PortabilityJob job, boolean isExport) {
    // Data type not provided in param, attempt to lookup the data type from storage
    return isExport ? job.exportService() : job.importService();
  }

  /**
   * Parse and validate the data type .
   */
  public static PortableDataType getDataType(String dataType) {
    Optional<PortableDataType> dataTypeOption = Enums
        .getIfPresent(PortableDataType.class, dataType);
    Preconditions.checkState(dataTypeOption.isPresent(), "Data type not found: %s", dataType);
    return dataTypeOption.get();
  }

  /**
   * Determines whether the current service is a valid service
   */
  public static boolean isValidService(String serviceName, boolean isExport) {
    if (!Strings.isNullOrEmpty(serviceName)) {
      // TODO: Use service registry to validate the service is valid for import or export
      return true;
    }
    return false;
  }
}
