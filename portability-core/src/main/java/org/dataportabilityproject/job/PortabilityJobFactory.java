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

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.IOException;
import org.dataportabilityproject.shared.PortableDataType;
import org.dataportabilityproject.spi.cloud.types.LegacyPortabilityJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides methods for the creation of new {@link LegacyPortabilityJob} objects in correct initial state.
 */
@Singleton
public class PortabilityJobFactory {
  private final Logger logger = LoggerFactory.getLogger(PortabilityJobFactory.class);

  @Inject public PortabilityJobFactory() {}

  /**
   * Creates a new user job in initial state with session key.
   */
  public LegacyPortabilityJob create(PortableDataType dataType, String exportService,
      String importService) throws IOException {
    String encodedSessionKey = SecretKeyGenerator.generateKeyAndEncode();
    LegacyPortabilityJob job =
        createInitialJob(encodedSessionKey, dataType, exportService, importService);
    logger.info("Creating new OldPortabilityJob to transfer {} from {} to {}",
        dataType, exportService, importService);
    return job;
  }

  /** Creates the initial data entry to persist. */
  private static LegacyPortabilityJob createInitialJob(String sessionKey,
      PortableDataType dataType, String exportService, String importService) {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(sessionKey), "sessionKey missing");
    Preconditions.checkArgument(!Strings.isNullOrEmpty(exportService), "exportService missing");
    Preconditions.checkArgument(!Strings.isNullOrEmpty(importService), "importService missing");
    Preconditions.checkNotNull(dataType, "dataType missing");
    return LegacyPortabilityJob.builder()
        .setDataType(dataType.name())
        .setExportService(exportService)
        .setImportService(importService)
        .setSessionKey(sessionKey)
        .build();
  }
}
