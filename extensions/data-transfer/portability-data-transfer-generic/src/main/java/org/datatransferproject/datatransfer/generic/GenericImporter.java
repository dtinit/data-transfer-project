package org.datatransferproject.datatransfer.generic;

import static java.lang.String.format;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.util.UUID;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.spi.transfer.idempotentexecutor.IdempotentImportExecutor;
import org.datatransferproject.spi.transfer.provider.ImportResult;
import org.datatransferproject.spi.transfer.provider.ImportResult.ResultType;
import org.datatransferproject.spi.transfer.provider.Importer;
import org.datatransferproject.types.common.models.ContainerResource;
import org.datatransferproject.types.common.models.photos.PhotoModel;
import org.datatransferproject.types.common.models.videos.VideoModel;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;

@FunctionalInterface
interface ContainerMapper<C extends ContainerResource, R> {
  public Iterable<ImportableData<R>> apply(C containerResource);
}

@JsonIgnoreProperties({
  "contentUrl",
  "fetchableUrl",
  "inTempStore",
  "identifier",
  "dataId",
  "headline"
})
abstract class MediaSkipFieldsMixin {}

public class GenericImporter<C extends ContainerResource, R>
    implements Importer<TokensAndUrlAuthData, C> {
  ContainerMapper<C, R> containerUnpacker;
  ObjectMapper om = new ObjectMapper();
  Monitor monitor;
  OkHttpClient client = new OkHttpClient();

  static final MediaType JSON = MediaType.parse("application/json");

  public GenericImporter(ContainerMapper<C, R> containerUnpacker, Monitor monitor) {
    this.monitor = monitor;
    this.containerUnpacker = containerUnpacker;
    om.registerModule(new JavaTimeModule());
    // TODO: this probably shouldn't live here
    om.addMixIn(VideoModel.class, MediaSkipFieldsMixin.class);
    om.addMixIn(PhotoModel.class, MediaSkipFieldsMixin.class);
  }

  @Override
  public ImportResult importItem(
      UUID jobId,
      IdempotentImportExecutor idempotentExecutor,
      TokensAndUrlAuthData authData,
      C data)
      throws Exception {
    for (ImportableData<?> importableData : containerUnpacker.apply(data)) {
      idempotentExecutor.executeAndSwallowIOExceptions(
          importableData.getIdempotentId(),
          importableData.getName(),
          () -> importSingleItem(importableData.getJsonData()));
    }
    return new ImportResult(ResultType.OK);
  }

  boolean importSingleItem(GenericPayload<?> dataItem) throws IOException {
    Request request =
        new Request.Builder()
            .url("http://localhost:8080") // TODO
            .post(RequestBody.create(JSON, om.writeValueAsBytes(dataItem)))
            .build();

    try (Response response = client.newCall(request).execute()) {
      if (response.code() >= 400) {
        throw new IOException(format("Error %s", response.code()));
      }
      return true;
    }
  }
}
