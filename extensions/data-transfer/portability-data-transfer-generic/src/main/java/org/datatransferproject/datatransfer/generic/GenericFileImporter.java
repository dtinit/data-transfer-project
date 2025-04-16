package org.datatransferproject.datatransfer.generic;

import static java.lang.String.format;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Optional;
import java.util.UUID;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.spi.cloud.connection.ConnectionProvider;
import org.datatransferproject.spi.cloud.storage.TemporaryPerJobDataStore;
import org.datatransferproject.spi.cloud.storage.TemporaryPerJobDataStore.InputStreamWrapper;
import org.datatransferproject.spi.transfer.types.DestinationMemoryFullException;
import org.datatransferproject.spi.transfer.types.InvalidTokenException;
import org.datatransferproject.types.common.models.ContainerResource;
import org.datatransferproject.types.transfer.auth.AppCredentials;
import org.datatransferproject.types.transfer.auth.AuthData;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;

public class GenericFileImporter<C extends ContainerResource, R> extends GenericImporter<C, R> {
  private TemporaryPerJobDataStore dataStore;
  private ConnectionProvider connectionProvider;

  static final MediaType MULTIPART_RELATED = MediaType.parse("multipart/related");
  static final MediaType OCTET_STREAM = MediaType.parse("application/octet-stream");

  public GenericFileImporter(
      ContainerSerializer<C, R> containerSerializer,
      AppCredentials appCredentials,
      URL endpoint,
      TemporaryPerJobDataStore dataStore,
      Monitor monitor) {
    super(containerSerializer, appCredentials, endpoint, monitor);
    this.dataStore = dataStore;
    this.connectionProvider = new ConnectionProvider(dataStore);
  }

  @Override
  public boolean importSingleItem(
      UUID jobId, TokensAndUrlAuthData authData, ImportableData<R> dataItem)
      throws IOException, InvalidTokenException, DestinationMemoryFullException {
    if (dataItem instanceof ImportableFileData) {
      return importSingleFileItem(jobId, authData, (ImportableFileData<R>) dataItem);
    } else {
      return super.importSingleItem(jobId, authData, dataItem);
    }
  }

  private <T> boolean importSingleFileItem(
      UUID jobId, AuthData authData, ImportableFileData<R> data)
      throws IOException, InvalidTokenException, DestinationMemoryFullException {
    InputStreamWrapper wrapper = connectionProvider.getInputStreamForItem(jobId, data.getFile());
    File tempFile =
        dataStore.getTempFileFromInputStream(wrapper.getStream(), data.getFile().getName(), null);
    MediaType mimeType =
        Optional.ofNullable(MediaType.parse(data.getFileMimeType())).orElse(OCTET_STREAM);
    Request request =
        new Request.Builder()
            .url(endpoint)
            .addHeader("Authorization", format("Bearer %s", authData.getToken()))
            .post(
                new MultipartBody.Builder()
                    .setType(MULTIPART_RELATED)
                    .addPart(RequestBody.create(JSON, om.writeValueAsBytes(data.getJsonData())))
                    .addPart(MultipartBody.create(mimeType, tempFile))
                    .build())
            .build();

    try (Response response = client.newCall(request).execute()) {
      return parseResponse(response);
    } finally {
      tempFile.delete();
    }
  }
}
