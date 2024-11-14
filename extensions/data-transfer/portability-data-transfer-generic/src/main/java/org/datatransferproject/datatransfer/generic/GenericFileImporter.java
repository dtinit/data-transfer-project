package org.datatransferproject.datatransfer.generic;

import static java.lang.String.format;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.File;
import java.io.IOException;
import java.util.UUID;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.spi.cloud.connection.ConnectionProvider;
import org.datatransferproject.spi.cloud.storage.TemporaryPerJobDataStore;
import org.datatransferproject.spi.cloud.storage.TemporaryPerJobDataStore.InputStreamWrapper;
import org.datatransferproject.spi.transfer.idempotentexecutor.IdempotentImportExecutor;
import org.datatransferproject.spi.transfer.provider.ImportResult;
import org.datatransferproject.spi.transfer.provider.ImportResult.ResultType;
import org.datatransferproject.types.common.DownloadableItem;
import org.datatransferproject.types.common.models.ContainerResource;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;

public class GenericFileImporter<C extends ContainerResource> extends GenericImporter<C> {
  private TemporaryPerJobDataStore dataStore;
  private ConnectionProvider connectionProvider;
  private OkHttpClient client = new OkHttpClient();

  static final MediaType MULTIPART_RELATED = MediaType.parse("multipart/related");
  static final MediaType OCTET_STREAM = MediaType.parse("application/octet-stream");

  public GenericFileImporter(
      ContainerMapper<C> containerUnpacker, TemporaryPerJobDataStore dataStore, Monitor monitor) {
    super(containerUnpacker, monitor);
    this.dataStore = dataStore;
    this.connectionProvider = new ConnectionProvider(dataStore);
  }

  @Override
  public ImportResult importItem(
      UUID jobId,
      IdempotentImportExecutor idempotentExecutor,
      TokensAndUrlAuthData authData,
      C data)
      throws Exception {
    for (ImportableData importableData : containerUnpacker.apply(data, om)) {
      idempotentExecutor.executeAndSwallowIOExceptions(
          importableData.getIdempotentId(),
          importableData.getName(),
          () ->
              importableData instanceof ImportableFileData
                  ? importSingleFileItem(
                      jobId,
                      importableData.getJsonData(),
                      ((ImportableFileData) importableData).getFile())
                  : importSingleItem(importableData.getJsonData()));
    }
    return new ImportResult(ResultType.OK);
  }

  private boolean importSingleFileItem(UUID jobId, JsonNode metadata, DownloadableItem file)
      throws IOException {
    InputStreamWrapper wrapper = connectionProvider.getInputStreamForItem(jobId, file);
    File tempFile =
        dataStore.getTempFileFromInputStream(
            wrapper.getStream(), file.getName(), /* TODO set extension from mimetype */ null);
    MediaType mimeType = OCTET_STREAM; // TODO set mimetype
    Request request =
        new Request.Builder()
            .url("http://localhost:8080") // TODO make configurable
            .post(
                new MultipartBody.Builder()
                    .setType(MULTIPART_RELATED)
                    .addPart(
                        new Headers.Builder().build(),
                        RequestBody.create(JSON, om.writeValueAsBytes(metadata)))
                    .addPart(
                        new Headers.Builder().build(), MultipartBody.create(mimeType, tempFile))
                    .build())
            .build();

    try (Response response = client.newCall(request).execute()) {
      if (response.code() >= 400) {
        throw new IOException(format("Error %s", response.code()));
      }
      return true;
    } finally {
      tempFile.delete();
    }
  }
}
