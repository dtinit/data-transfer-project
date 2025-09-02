package org.datatransferproject.datatransfer.generic;

import static java.lang.String.format;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.UUID;
import okhttp3.Headers;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.apache.commons.fileupload.FileUploadBase.FileUploadIOException;
import org.apache.commons.fileupload.MultipartStream;
import org.apache.commons.fileupload.MultipartStream.MalformedStreamException;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.spi.cloud.storage.TemporaryPerJobDataStore;
import org.datatransferproject.spi.transfer.idempotentexecutor.InMemoryIdempotentImportExecutor;
import org.datatransferproject.transfer.JobMetadata;
import org.datatransferproject.types.common.models.IdOnlyContainerResource;
import org.datatransferproject.types.transfer.auth.AppCredentials;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class GenericFileImporterTest {
  private MockWebServer webServer;
  private Monitor monitor = new Monitor() {};
  private TemporaryPerJobDataStore dataStore =
      new TemporaryPerJobDataStore() {
        @Override
        public InputStreamWrapper getStream(UUID jobId, String key) throws IOException {
          return new InputStreamWrapper(new ByteArrayInputStream("Hello world".getBytes()));
        }
      };
  private static final UUID MOCK_JOB_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
  private static final String MOCK_EXPORT_SERVICE = "mockExportService";

  @Before
  public void setup() throws IOException {
    webServer = new MockWebServer();
    webServer.start();
  }

  @After
  public void teardown() throws IOException {
    webServer.shutdown();
  }

  public static MultipartStream getMultipartStream(RecordedRequest request) {
    assertTrue(
        format("Invalid Content-Type '%s'", request.getHeader("Content-Type")),
        request.getHeader("Content-Type").startsWith("multipart/related"));
    String boundaryString = request.getHeader("Content-Type").split(";", 2)[1].strip();
    assertTrue(
        format("Invalid boundary string '%s'", boundaryString),
        boundaryString.startsWith("boundary="));
    String boundary = boundaryString.split("=", 2)[1];

    return new MultipartStream(
        request.getBody().inputStream(),
        boundary.getBytes(),
        Integer.parseInt(request.getHeader("Content-Length")),
        null);
  }

  Headers readPartHeaders(MultipartStream stream)
      throws FileUploadIOException, MalformedStreamException {
    Headers.Builder builder = new Headers.Builder();
    stream.readHeaders().strip().lines().forEach(line -> builder.add(line));
    return builder.build();
  }

  public static String readPartBody(MultipartStream stream) throws MalformedStreamException, IOException {
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    stream.readBodyData(os);
    return new String(os.toByteArray(), StandardCharsets.UTF_8);
  }

  @Test
  public void testGenericFileImporter() throws Exception {
    GenericFileImporter<IdOnlyContainerResource, String> importer =
        new GenericFileImporter<>(
            container ->
                Arrays.asList(
                    new ImportableFileData<>(
                        new CachedDownloadableItem(container.getId(), container.getId()),
                        "video/mp4",
                        new GenericPayload<>(container.getId(), "schemasource"),
                        container.getId(),
                        container.getId())),
            new AppCredentials("key", "secret"),
            webServer.url("/id").url(),
            dataStore,
            monitor);
    InMemoryIdempotentImportExecutor executor = new InMemoryIdempotentImportExecutor(monitor);
    webServer.enqueue(new MockResponse().setResponseCode(201).setBody("OK"));

    importer.importItem(
        UUID.randomUUID(),
        executor,
        new TokensAndUrlAuthData(
            "accessToken", "refreshToken", webServer.url("/refresh").toString()),
        new IdOnlyContainerResource("id"));

    assertEquals(1, webServer.getRequestCount());

    RecordedRequest request = webServer.takeRequest();
    MultipartStream stream = getMultipartStream(request);

    assertTrue("Missing JSON part", stream.skipPreamble());
    assertEquals("application/json", readPartHeaders(stream).get("Content-Type"));
    assertEquals(
        "{\"@type\":\"GenericPayload\",\"payload\":\"id\",\"schemaSource\":\"schemasource\",\"apiVersion\":\"0.1.0\"}",
        readPartBody(stream));

    assertTrue("Missing file part", stream.readBoundary());
    assertEquals("video/mp4", readPartHeaders(stream).get("Content-Type"));
    assertEquals("Hello world", readPartBody(stream));

    assertFalse("Unexpected extra data", stream.readBoundary());
  }

  @Test
  public void testGenericFileImporterMixedTypes() throws Exception {
    GenericFileImporter<IdOnlyContainerResource, String> importer =
        new GenericFileImporter<>(
            container ->
                Arrays.asList(
                    new ImportableFileData<>(
                        new CachedDownloadableItem("file", "file"),
                        "invalid_mimetype",
                        new GenericPayload<>("file", "schemasource"),
                        "file",
                        "file"),
                    new ImportableData<>(
                        new GenericPayload<>("not file", "schemasource"), "not file", "not file")),
            new AppCredentials("key", "secret"),
            webServer.url("/id").url(),
            dataStore,
            monitor);
    InMemoryIdempotentImportExecutor executor = new InMemoryIdempotentImportExecutor(monitor);
    webServer.enqueue(new MockResponse().setResponseCode(201).setBody("OK"));
    webServer.enqueue(new MockResponse().setResponseCode(201).setBody("OK"));

    importer.importItem(
        UUID.randomUUID(),
        executor,
        new TokensAndUrlAuthData(
            "accessToken", "refreshToken", webServer.url("/refresh").toString()),
        new IdOnlyContainerResource("id"));

    assertEquals(2, webServer.getRequestCount());

    RecordedRequest fileRequest = webServer.takeRequest();
    MultipartStream stream = getMultipartStream(fileRequest);
    assertTrue("Missing JSON part", stream.skipPreamble());
    assertEquals("application/json", readPartHeaders(stream).get("Content-Type"));
    assertEquals(
        "{\"@type\":\"GenericPayload\",\"payload\":\"file\",\"schemaSource\":\"schemasource\",\"apiVersion\":\"0.1.0\"}",
        readPartBody(stream));
    assertTrue("Missing file part", stream.readBoundary());
    assertEquals("application/octet-stream", readPartHeaders(stream).get("Content-Type"));
    assertEquals("Hello world", readPartBody(stream));
    assertFalse("Unexpected extra data", stream.readBoundary());

    RecordedRequest jsonRequest = webServer.takeRequest();
    assertEquals("application/json", jsonRequest.getHeader("Content-Type"));
    assertEquals(
        "{\"@type\":\"GenericPayload\",\"payload\":\"not"
            + " file\",\"schemaSource\":\"schemasource\",\"apiVersion\":\"0.1.0\"}",
        jsonRequest.getBody().readUtf8());
  }

  @Test
  public void testGenericFileImporterPassingJobMetadata() throws Exception {
    try (var mockedStatic = Mockito.mockStatic(JobMetadata.class)) {
      mockedStatic.when(JobMetadata::getJobId).thenReturn(this.MOCK_JOB_ID);
      mockedStatic.when(JobMetadata::getExportService).thenReturn(this.MOCK_EXPORT_SERVICE);
GenericFileImporter<IdOnlyContainerResource, String> importer =
        new GenericFileImporter<>(
            container ->
                Arrays.asList(
                    new ImportableFileData<>(
                        new CachedDownloadableItem(container.getId(), container.getId()),
                        "video/mp4",
                        new GenericPayload<>(container.getId(), "schemasource"),
                        container.getId(),
                        container.getId())),
            new AppCredentials("key", "secret"),
            webServer.url("/id").url(),
            dataStore,
            monitor);
      InMemoryIdempotentImportExecutor executor = new InMemoryIdempotentImportExecutor(monitor);
      webServer.enqueue(new MockResponse().setResponseCode(201).setBody("OK"));

      importer.importItem(
          UUID.randomUUID(),
          executor,
          new TokensAndUrlAuthData(
              "accessToken", "refreshToken", webServer.url("/refresh").toString()),
          new IdOnlyContainerResource("id"));

      assertEquals(1, webServer.getRequestCount());

      RecordedRequest request = webServer.takeRequest();
      MultipartStream stream = getMultipartStream(request);

      assertEquals(1, webServer.getRequestCount());
      RecordedRequest request = webServer.takeRequest();
      assertEquals("POST", request.getMethod());
      assertEquals(this.MOCK_EXPORT_SERVICE, request.getHeader("X-DTP-Export-Service"));
      assertEquals(MOCK_JOB_ID.toString(), request.getHeader("X-DTP-Job-Id"));
      assertTrue(executor.getErrors().isEmpty());
    }
  }
}
