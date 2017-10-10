package org.dataportabilityproject.job;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import java.io.IOException;
import org.dataportabilityproject.cloud.interfaces.PersistentKeyValueStore;
import org.dataportabilityproject.shared.PortableDataType;

/**
 * Provides methods for the creation of new {@link PortabilityJob} objects in correct initial state.
 */
public class PortabilityJobFactory {
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

  /** Creates a new user job in initial state. */
  public PortabilityJob create(PortableDataType dataType, String exportService,
      String importService) throws IOException {
    String newId = idProvider.createId();
    PortabilityJob job = createInitialJob(newId, dataType, exportService, importService);
    System.out.println("Creating new PortabilityJob, id: " + newId);
    return job;
  }

  /** Creates the initial data entry to persist. */
  private static PortabilityJob createInitialJob(String id, PortableDataType dataType,
      String exportService, String importService) {
    Preconditions.checkArgument(Strings.isNullOrEmpty(id), "id missing");
    Preconditions.checkArgument(Strings.isNullOrEmpty(exportService), "exportService missing");
    Preconditions.checkArgument(Strings.isNullOrEmpty(importService), "importService missing");
    Preconditions.checkNotNull(dataType, "dataType missing");
    return PortabilityJob.builder()
      .setId(id)
      .setDataType(dataType.name())
      .setExportService(exportService)
      .setImportService(importService)
      .build();
  }
}
