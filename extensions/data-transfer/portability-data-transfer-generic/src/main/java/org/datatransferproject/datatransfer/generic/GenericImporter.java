package org.datatransferproject.datatransfer.generic;

import static java.lang.String.format;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.annotations.VisibleForTesting;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.datatransfer.generic.auth.OAuthTokenManager;
import org.datatransferproject.spi.transfer.idempotentexecutor.IdempotentImportExecutor;
import org.datatransferproject.spi.transfer.provider.ImportResult;
import org.datatransferproject.spi.transfer.provider.ImportResult.ResultType;
import org.datatransferproject.spi.transfer.provider.Importer;
import org.datatransferproject.spi.transfer.types.DestinationMemoryFullException;
import org.datatransferproject.spi.transfer.types.InvalidTokenException;
import org.datatransferproject.types.common.models.ContainerResource;
import org.datatransferproject.types.transfer.auth.AppCredentials;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;

public class GenericImporter<C extends ContainerResource, R>
    implements Importer<TokensAndUrlAuthData, C> {

  @JsonIgnoreProperties(ignoreUnknown = true)
  static class ErrorResponse {
    private final String error;
    private final Optional<String> errorDescription;

    @JsonCreator
    public ErrorResponse(
        @JsonProperty(value = "error", required = true) String error,
        @Nullable @JsonProperty("error_description") String errorDescription) {
      this.error = error;
      this.errorDescription = Optional.ofNullable(errorDescription);
    }

    public String getError() {
      return error;
    }

    public Optional<String> getErrorDescription() {
      return errorDescription;
    }

    public String toString() {
      StringBuilder builder = new StringBuilder();
      builder.append(error);
      if (errorDescription.isPresent()) {
        builder.append(" - ");
        builder.append(errorDescription.get());
      }
      return builder.toString();
    }
  }

  ContainerSerializer<C, R> containerSerializer;
  URL endpoint;
  Monitor monitor;
  AppCredentials appCredentials;
  OkHttpClient client = new OkHttpClient();
  ObjectMapper om = new ObjectMapper();
  Map<UUID, OAuthTokenManager> jobTokenManagerMap = new HashMap<>();

  static final MediaType JSON = MediaType.parse("application/json");

  public GenericImporter(
      ContainerSerializer<C, R> containerSerializer,
      AppCredentials appCredentials,
      URL endpoint,
      Monitor monitor) {
    this.monitor = monitor;
    this.appCredentials = appCredentials;
    this.endpoint = endpoint;
    this.containerSerializer = containerSerializer;
    configureObjectMapper(om);
  }

  @VisibleForTesting
  static void configureObjectMapper(ObjectMapper objectMapper) {
    // ZonedDateTime and friends
    objectMapper.registerModule(new JavaTimeModule());
    // Optional fields
    objectMapper.registerModule(new Jdk8Module());
    // ISO timestamps rather than unix epoch seconds
    objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
  }

  @Override
  public ImportResult importItem(
      UUID jobId,
      IdempotentImportExecutor idempotentExecutor,
      TokensAndUrlAuthData initialAuthData,
      C data)
      throws Exception {
    OAuthTokenManager tokenManager =
        jobTokenManagerMap.computeIfAbsent(
            jobId,
            ignored -> new OAuthTokenManager(initialAuthData, appCredentials, client, monitor));
    for (ImportableData<R> importableData : containerSerializer.apply(data)) {
      idempotentExecutor.executeAndSwallowIOExceptions(
          importableData.getIdempotentId(),
          importableData.getName(),
          () ->
              tokenManager.withAuthData(
                  authData -> importSingleItem(jobId, authData, importableData)));
    }
    return new ImportResult(ResultType.OK);
  }

  boolean parseResponse(Response response) throws IOException, InvalidTokenException, DestinationMemoryFullException {
    if (response.code() >= 400) {
      byte[] body = response.body().bytes();
      ErrorResponse error;
      try {
        error = om.readValue(body, ErrorResponse.class);
      } catch (JsonParseException | JsonMappingException e) {
        throw new IOException(
            format(
                "Unexpected response (%d) '%s'",
                response.code(), new String(body, StandardCharsets.UTF_8)),
            e);
      }

      if (response.code() == 401 && error.getError().equals("invalid_token")) {
        throw new InvalidTokenException(error.toString(), null);
      } if (response.code() == 413 && error.getError().equals("destination_full")) {
        throw new DestinationMemoryFullException(
            String.format("Generic importer failed with code (%s)", response.code()),
            new RuntimeException("destination_full"));
      } else {
        throw new IOException(format("Error (%d) %s", response.code(), error.toString()));
      }
    }
    if (response.code() < 200 || response.code() >= 300) {
      throw new IOException(format("Unexpected response code (%d)", response.code()));
    }
    return true;
  }

  boolean importSingleItem(UUID jobId, TokensAndUrlAuthData authData, ImportableData<R> dataItem)
      throws IOException, InvalidTokenException, DestinationMemoryFullException {

    Request request =
        new Request.Builder()
            .url(endpoint)
            .addHeader("Authorization", format("Bearer %s", authData.getToken()))
            .post(RequestBody.create(JSON, om.writeValueAsBytes(dataItem.getJsonData())))
            .build();

    try (Response response = client.newCall(request).execute()) {
      return parseResponse(response);
    }
  }
}
