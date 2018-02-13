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
import java.util.UUID;
import org.dataportabilityproject.shared.PortableDataType;
import org.dataportabilityproject.shared.ServiceMode;
import org.dataportabilityproject.spi.cloud.types.LegacyPortabilityJob;
import org.dataportabilityproject.types.transfer.auth.AuthData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility methods for handling data in the related to jobss.
 */
public final class JobUtils {
  private static final Logger logger = LoggerFactory.getLogger(JobUtils.class);

  public static String decodeBase64(String encoded) {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(encoded));
    return new String(BaseEncoding.base64Url().decode(encoded), Charsets.UTF_8);
  }

  public static UUID decodeJobId(String encodedJobId) {
    return UUID.fromString(decodeBase64(encodedJobId));
  }

  public static String encodeJobId(UUID jobId) {
    Preconditions.checkNotNull(jobId);
    return BaseEncoding.base64Url().encode(jobId.toString().getBytes(Charsets.UTF_8));
  }

  /* Returns the initial auth data for export or import determined by the {@code serviceMode} param. */
  public static AuthData getInitialAuthData(LegacyPortabilityJob job, ServiceMode serviceMode) {
    switch (serviceMode) {
      case EXPORT:
        return job.exportInitialAuthData();
      case IMPORT:
        return job.importInitialAuthData();
      default:
        throw new IllegalArgumentException("Unsupported service mode: " + serviceMode);
    }
  }

  /* Sets the service in the correct field of the OldPortabilityJob */
  public static LegacyPortabilityJob setAuthData(LegacyPortabilityJob job, AuthData authData,
      ServiceMode serviceMode) {
    LegacyPortabilityJob.Builder updatedJob = job.toBuilder();
    switch (serviceMode) {
      case EXPORT:
        updatedJob.setExportAuthData(authData);
        break;
      case IMPORT:
        updatedJob.setImportAuthData(authData);
        break;
      default:
        throw new IllegalArgumentException("Unsupported service mode: " + serviceMode);
    }
    return updatedJob.build();
  }

  /* Sets the service in the correct field of the OldPortabilityJob */
  public static LegacyPortabilityJob setInitialAuthData(LegacyPortabilityJob job, AuthData initialAuthData,
      ServiceMode serviceMode) {
    logger.debug("Setting initialAuthData: {}, serviceMode: {}", initialAuthData, serviceMode);
    LegacyPortabilityJob.Builder updatedJob = job.toBuilder();
    switch (serviceMode) {
      case EXPORT:
        updatedJob.setExportInitialAuthData(initialAuthData);
        break;
      case IMPORT:
        updatedJob.setImportInitialAuthData(initialAuthData);
        break;
      default:
        throw new IllegalArgumentException("Unsupported service mode: " + serviceMode);
    }
    return updatedJob.build();
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
  public static boolean isValidService(String serviceName, ServiceMode serviceMode) {
    if (!Strings.isNullOrEmpty(serviceName)) {
      // TODO: Use service registry to validate the service is valid for import or export
      return true;
    }
    return false;
  }
}
