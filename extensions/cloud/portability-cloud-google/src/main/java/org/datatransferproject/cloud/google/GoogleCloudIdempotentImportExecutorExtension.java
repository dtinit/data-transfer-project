package org.datatransferproject.cloud.google;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import java.io.IOException;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.spi.transfer.idempotentexecutor.IdempotentImportExecutor;
import org.datatransferproject.spi.transfer.idempotentexecutor.IdempotentImportExecutorExtension;

public class GoogleCloudIdempotentImportExecutorExtension implements
    IdempotentImportExecutorExtension {

  private Datastore datastore;

  @Override
  public IdempotentImportExecutor getIdempotentImportExecutor(Monitor monitor) throws IOException {
    try {

      return new GoogleCloudIdempotentImportExecutor(getDatastore(), monitor);
    } catch (IOException e) {
      monitor.severe(() -> "Error initializing datastore: " + e);
      throw e;
    }
  }

  @Override
  public void initialize() {

  }

  private synchronized Datastore getDatastore() throws IOException {
    if (datastore == null) {
      datastore = DatastoreOptions.newBuilder()
          .setProjectId(GoogleCloudUtils.getProjectId())
          .setCredentials(GoogleCredentials.getApplicationDefault())
          .build()
          .getService();
    }

    return datastore;
  }
}
