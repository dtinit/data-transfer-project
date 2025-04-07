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
package org.datatransferproject.transfer;

import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;

import java.util.UUID;
import org.datatransferproject.types.common.models.DataVertical;

/**
 * A class that contains metadata for a transfer worker's job.
 *
 * <p>
 *
 * <p>This class is completely static to ensure it is a singleton within each transfer worker
 * instance.
 */
@SuppressWarnings("WeakerAccess")
// We make the class and various methods public so they can be accessed from Monitors
public final class JobMetadata {
  private static byte[] encodedPrivateKey = null;
  private static UUID jobId = null;
  private static DataVertical dataType = null;
  private static String exportService = null;
  private static String importService = null;
  private static Stopwatch stopWatch = null;

  public static boolean isInitialized() {
    return (jobId != null
        && encodedPrivateKey != null
        && dataType != null
        && exportService != null
        && importService != null
        && stopWatch != null);
  }

  static void init(
      UUID initJobId,
      byte[] initEncodedPrivateKey,
      DataVertical initDataType,
      String initExportService,
      String initImportService,
      Stopwatch initStopWatch) {
    Preconditions.checkState(!isInitialized(), "JobMetadata cannot be initialized twice");
    jobId = initJobId;
    encodedPrivateKey = initEncodedPrivateKey;
    dataType = initDataType;
    exportService = initExportService;
    importService = initImportService;
    stopWatch = initStopWatch;
  }

  // TODO: remove this
  static synchronized void reset() {
    jobId = null;
    encodedPrivateKey = null;
    dataType = null;
    exportService = null;
    importService = null;
    stopWatch = null;
  }

  static byte[] getPrivateKey() {
    Preconditions.checkState(isInitialized(), "JobMetadata must be initialized");
    return encodedPrivateKey;
  }

  public static UUID getJobId() {
    Preconditions.checkState(isInitialized(), "JobMetadata must be initialized");
    return jobId;
  }

  public static DataVertical getDataType() {
    Preconditions.checkState(isInitialized(), "JobMetadata must be initialized");
    return dataType;
  }

  public static String getExportService() {
    Preconditions.checkState(isInitialized(), "JobMetadata must be initialized");
    return exportService;
  }

  public static String getImportService() {
    Preconditions.checkState(isInitialized(), "JobMetadata must be initialized");
    return importService;
  }

  public static Stopwatch getStopWatch() {
    Preconditions.checkState(isInitialized(), "JobMetadata must be initialized");
    return stopWatch;
  }
}
