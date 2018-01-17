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
import java.io.IOException;
import org.dataportabilityproject.shared.PortableDataType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides methods for the creation of new {@link PortabilityJob} objects in correct initial state.
 */
public class PortabilityJobFactory {
  private final Logger logger = LoggerFactory.getLogger(PortabilityJobFactory.class);
  // Keys for specific values in data store
  private static final String ID_DATA_KEY = "UUID";
  private static final String TOKEN_DATA_KEY = "TOKEN";
  private static final String DATA_TYPE_DATA_KEY = "DATA_TYPE";
  private static final String EXPORT_SERVICE_DATA_KEY = "EXPORT_SERVICE";
  private static final String EXPORT_ACCOUNT_DATA_KEY = "EXPORT_ACCOUNT";
  private static final String EXPORT_INITIAL_AUTH_DATA_KEY = "EXPORT_INITIAL_AUTH_DATA";
  private static final String EXPORT_AUTH_DATA_KEY = "EXPORT_AUTH_DATA";
  private static final String IMPORT_SERVICE_DATA_KEY = "IMPORT_SERVICE";
  private static final String IMPORT_ACCOUNT_DATA_KEY = "IMPORT_ACCOUNT";
  private static final String IMPORT_INITIAL_AUTH_DATA_KEY = "IMPORT_INITIAL_AUTH_DATA";
  private static final String IMPORT_AUTH_DATA_KEY = "IMPORT_AUTH_DATA";

  private final IdProvider idProvider;

  public PortabilityJobFactory(IdProvider idProvider) {
    this.idProvider = idProvider;
  }

  /**
   * Creates a new user job in initial state with session key.
   */
  public PortabilityJob create(PortableDataType dataType, String exportService,
      String importService) throws IOException {
    String newId = idProvider.createId();
    String encodedSessionKey = SecretKeyGenerator.generateKeyAndEncode();
    PortabilityJob job = createInitialJob(newId, encodedSessionKey, dataType, exportService, importService);
    logger.info("Creating new PortabilityJob, id: {}", newId);
    return job;
  }

  /** Creates the initial data entry to persist. */
  private static PortabilityJob createInitialJob(String id, String sessionKey,
      PortableDataType dataType, String exportService, String importService) {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(sessionKey), "sessionKey missing");
    Preconditions.checkArgument(!Strings.isNullOrEmpty(id), "id missing");
    Preconditions.checkArgument(!Strings.isNullOrEmpty(exportService), "exportService missing");
    Preconditions.checkArgument(!Strings.isNullOrEmpty(importService), "importService missing");
    Preconditions.checkNotNull(dataType, "dataType missing");
    return PortabilityJob.builder()
        .setId(id)
        .setDataType(dataType.name())
        .setExportService(exportService)
        .setImportService(importService)
        .setSessionKey(sessionKey)
        .build();
  }

  /**
   * Creates the initial data entry to persist.
   * @deprecated Remove when encrypted flow complete.
   */
  @Deprecated
  private static PortabilityJob createInitialJob(String id, PortableDataType dataType,
      String exportService, String importService) {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(id), "id missing");
    Preconditions.checkArgument(!Strings.isNullOrEmpty(exportService), "exportService missing");
    Preconditions.checkArgument(!Strings.isNullOrEmpty(importService), "importService missing");
    Preconditions.checkNotNull(dataType, "dataType missing");
    return PortabilityJob.builder()
      .setId(id)
      .setDataType(dataType.name())
      .setExportService(exportService)
      .setImportService(importService)
      .build();
  }
}
