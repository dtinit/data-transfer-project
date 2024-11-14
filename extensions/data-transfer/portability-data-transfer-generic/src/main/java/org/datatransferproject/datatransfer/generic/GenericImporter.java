package org.datatransferproject.datatransfer.generic;

import static java.lang.String.format;

import com.fasterxml.jackson.databind.JsonNode;
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
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;

@FunctionalInterface
interface ContainerMapper<C extends ContainerResource> {
  public Iterable<ImportableData> apply(C containerResource, ObjectMapper om);
}

public class GenericImporter<C extends ContainerResource>
    implements Importer<TokensAndUrlAuthData, C> {
  ContainerMapper<C> containerUnpacker;
  ObjectMapper om = new ObjectMapper();
  Monitor monitor;
  OkHttpClient client = new OkHttpClient();

  static final MediaType JSON = MediaType.parse("application/json");

  public GenericImporter(ContainerMapper<C> containerUnpacker, Monitor monitor) {
    this.monitor = monitor;
    this.containerUnpacker = containerUnpacker;
    om.registerModule(new JavaTimeModule());
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
          () -> importSingleItem(importableData.getJsonData()));
    }
    return new ImportResult(ResultType.OK);
  }

  boolean importSingleItem(JsonNode dataItem) throws IOException {
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
