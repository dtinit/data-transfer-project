package org.datatransferproject.datatransfer.generic;

import static java.lang.String.format;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.spi.cloud.storage.TemporaryPerJobDataStore;
import org.datatransferproject.spi.transfer.idempotentexecutor.InMemoryIdempotentImportExecutor;
import org.datatransferproject.spi.transfer.types.DestinationMemoryFullException;
import org.datatransferproject.types.common.models.IdOnlyContainerResource;
import org.datatransferproject.types.transfer.auth.AppCredentials;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;
import org.datatransferproject.types.transfer.errors.ErrorDetail;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class GenericImporterTest {
  private final Monitor monitor = new Monitor() {};
  private final TemporaryPerJobDataStore dataStore = new TemporaryPerJobDataStore() {};
  @Parameter public String importerClass;
  private MockWebServer webServer;

  @Parameters(name = "{0}")
  public static Collection<String> strings() {
    return Arrays.asList(GenericImporter.class.getName(), GenericFileImporter.class.getName());
  }

  static void assertContains(String expected, String actual) {
    assertTrue(
        format("Missing substring [%s] from [%s]", expected, actual), actual.contains(expected));
  }

  @Before
  public void setup() throws IOException {
    webServer = new MockWebServer();
    webServer.start();
  }

  @After
  public void teardown() throws IOException {
    webServer.shutdown();
  }

  <C> GenericImporter<IdOnlyContainerResource, C> getImporter(
      String cls, ContainerSerializer<IdOnlyContainerResource, C> containerSerializer) {
    if (cls.equals(GenericFileImporter.class.getName())) {
      return new GenericFileImporter<>(
          containerSerializer,
          new AppCredentials("key", "secret"),
          webServer.url("/id").url(),
          dataStore,
          monitor);
    } else {
      return new GenericImporter<>(
          containerSerializer,
          new AppCredentials("key", "secret"),
          webServer.url("/id").url(),
          monitor);
    }
  }

  Dispatcher getDispatcher() {
    return new Dispatcher() {
      @Override
      public MockResponse dispatch(RecordedRequest request) {
        switch (request.getPath()) {
          case "/id":
            if (request.getHeader("Authorization").equals("Bearer invalidToken")) {
              return new MockResponse()
                  .setResponseCode(401)
                  .setBody("{\"error\":\"invalid_token\"}");
            }
            return new MockResponse().setResponseCode(201).setBody("OK");
          case "/refresh":
            return new MockResponse()
                .setResponseCode(200)
                .setBody("{\"access_token\":\"newAccessToken\",\"token_type\":\"Bearer\"}");
          default:
            return new MockResponse().setResponseCode(404);
        }
      }
    };
  }

  @Test
  public void testGenericImporter() throws Exception {
    InMemoryIdempotentImportExecutor executor = new InMemoryIdempotentImportExecutor(monitor);
    GenericImporter<IdOnlyContainerResource, String> importer =
        getImporter(
            importerClass,
            container ->
                    List.of(
                            new ImportableData<>(
                                    new GenericPayload<>(container.getId(), "schemasource"),
                                    container.getId(),
                                    container.getId())));
    webServer.setDispatcher(getDispatcher());

    importer.importItem(
        UUID.randomUUID(),
        executor,
        new TokensAndUrlAuthData(
            "accessToken", "refreshToken", webServer.url("/refresh").toString()),
        new IdOnlyContainerResource("id"));

    assertEquals(1, webServer.getRequestCount());
    RecordedRequest request = webServer.takeRequest();
    assertEquals("POST", request.getMethod());
    assertEquals("Bearer accessToken", request.getHeader("Authorization"));
    assertEquals(
        "{\"@type\":\"GenericPayload\",\"payload\":\"id\",\"schemaSource\":\"schemasource\",\"apiVersion\":\"0.1.0\"}",
        new String(request.getBody().readByteArray(), StandardCharsets.UTF_8));
    assertTrue(executor.getErrors().isEmpty());
  }

  @Test
  public void testGenericImporterMultipleItems() throws Exception {
    GenericImporter<IdOnlyContainerResource, Integer> importer =
        getImporter(
            importerClass,
            container ->
                Arrays.asList(
                    new ImportableData<>(new GenericPayload<>(1, "schemasource"), "id1", "id1"),
                    new ImportableData<>(new GenericPayload<>(2, "schemasource"), "id2", "id2")));
    webServer.setDispatcher(getDispatcher());

    importer.importItem(
        UUID.randomUUID(),
        new InMemoryIdempotentImportExecutor(monitor),
        new TokensAndUrlAuthData(
            "accessToken", "refreshToken", webServer.url("/refresh").toString()),
        new IdOnlyContainerResource("id"));

    assertEquals(2, webServer.getRequestCount());

    RecordedRequest request1 = webServer.takeRequest();
    assertEquals("POST", request1.getMethod());
    assertEquals("Bearer accessToken", request1.getHeader("Authorization"));
    assertEquals(
        "{\"@type\":\"GenericPayload\",\"payload\":1,\"schemaSource\":\"schemasource\",\"apiVersion\":\"0.1.0\"}",
        new String(request1.getBody().readByteArray(), StandardCharsets.UTF_8));

    RecordedRequest request2 = webServer.takeRequest();
    assertEquals("POST", request2.getMethod());
    assertEquals("Bearer accessToken", request2.getHeader("Authorization"));
    assertEquals(
        "{\"@type\":\"GenericPayload\",\"payload\":2,\"schemaSource\":\"schemasource\",\"apiVersion\":\"0.1.0\"}",
        new String(request2.getBody().readByteArray(), StandardCharsets.UTF_8));
  }

  @Test
  public void testGenericImporterMultipleItemsWithSameID() throws Exception {
    GenericImporter<IdOnlyContainerResource, Integer> importer =
        getImporter(
            importerClass,
            container ->
                Arrays.asList(
                    new ImportableData<>(new GenericPayload<>(1, "schemasource"), "id1", "id1"),
                    new ImportableData<>(new GenericPayload<>(1, "schemasource"), "id1", "id1")));
    webServer.setDispatcher(getDispatcher());

    importer.importItem(
        UUID.randomUUID(),
        new InMemoryIdempotentImportExecutor(monitor),
        new TokensAndUrlAuthData(
            "accessToken", "refreshToken", webServer.url("/refresh").toString()),
        new IdOnlyContainerResource("id"));

    assertEquals(1, webServer.getRequestCount());
    RecordedRequest request = webServer.takeRequest();
    assertEquals("POST", request.getMethod());
    assertEquals("Bearer accessToken", request.getHeader("Authorization"));
    assertEquals(
        "{\"@type\":\"GenericPayload\",\"payload\":1,\"schemaSource\":\"schemasource\",\"apiVersion\":\"0.1.0\"}",
        new String(request.getBody().readByteArray(), StandardCharsets.UTF_8));
  }

  @Test
  public void testGenericImporterTokenRefresh() throws Exception {
    GenericImporter<IdOnlyContainerResource, String> importer =
        getImporter(
            importerClass,
            container ->
                    List.of(
                            new ImportableData<>(
                                    new GenericPayload<>(container.getId(), "schemasource"),
                                    container.getId(),
                                    container.getId())));
    webServer.setDispatcher(getDispatcher());

    importer.importItem(
        UUID.randomUUID(),
        new InMemoryIdempotentImportExecutor(monitor),
        new TokensAndUrlAuthData(
            "invalidToken", "refreshToken", webServer.url("/refresh").toString()),
        new IdOnlyContainerResource("id"));

    assertEquals(3, webServer.getRequestCount());

    RecordedRequest request1 = webServer.takeRequest();
    assertEquals("POST", request1.getMethod());
    assertEquals("Bearer invalidToken", request1.getHeader("Authorization"));
    assertEquals(
        "{\"@type\":\"GenericPayload\",\"payload\":\"id\",\"schemaSource\":\"schemasource\",\"apiVersion\":\"0.1.0\"}",
        new String(request1.getBody().readByteArray(), StandardCharsets.UTF_8));

    RecordedRequest refreshRequest = webServer.takeRequest();
    assertEquals("POST", refreshRequest.getMethod());
    assertEquals(
        "grant_type=refresh_token&client_id=key&client_secret=secret&refresh_token=refreshToken",
        new String(refreshRequest.getBody().readByteArray(), StandardCharsets.UTF_8));

    RecordedRequest request2 = webServer.takeRequest();
    assertEquals("POST", request2.getMethod());
    assertEquals("Bearer newAccessToken", request2.getHeader("Authorization"));
    assertEquals(
        "{\"@type\":\"GenericPayload\",\"payload\":\"id\",\"schemaSource\":\"schemasource\",\"apiVersion\":\"0.1.0\"}",
        new String(request2.getBody().readByteArray(), StandardCharsets.UTF_8));
  }

  @Test
  public void testGenericImporterBadRequest() throws Exception {
    InMemoryIdempotentImportExecutor executor = new InMemoryIdempotentImportExecutor(monitor);
    GenericImporter<IdOnlyContainerResource, String> importer =
        getImporter(
            importerClass,
            container ->
                    List.of(
                            new ImportableData<>(
                                    new GenericPayload<>(container.getId(), "schemasource"),
                                    container.getId(),
                                    container.getId())));
    webServer.enqueue(
        new MockResponse().setResponseCode(400).setBody("{\"error\":\"bad_request\"}"));

    importer.importItem(
        UUID.randomUUID(),
        executor,
        new TokensAndUrlAuthData(
            "accessToken", "refreshToken", webServer.url("/refresh").toString()),
        new IdOnlyContainerResource("itemId"));

    Collection<ErrorDetail> errors = executor.getErrors();
    assertEquals(1, errors.size());
    ErrorDetail error = errors.iterator().next();
    assertEquals("itemId", error.title());
    assertContains("(400) bad_request", error.exception());
  }

  @Test
  public void testGenericImporterUnexpectedResponse() throws Exception {
    InMemoryIdempotentImportExecutor executor = new InMemoryIdempotentImportExecutor(monitor);
    GenericImporter<IdOnlyContainerResource, String> importer =
        getImporter(
            importerClass,
            container ->
                    List.of(
                            new ImportableData<>(
                                    new GenericPayload<>(container.getId(), "schemasource"),
                                    container.getId(),
                                    container.getId())));
    webServer.enqueue(new MockResponse().setResponseCode(400).setBody("notjson"));

    importer.importItem(
        UUID.randomUUID(),
        executor,
        new TokensAndUrlAuthData(
            "accessToken", "refreshToken", webServer.url("/refresh").toString()),
        new IdOnlyContainerResource("itemId"));

    Collection<ErrorDetail> errors = executor.getErrors();
    assertEquals(1, errors.size());
    ErrorDetail error = errors.iterator().next();
    assertEquals("itemId", error.title());
    assertContains("Unexpected response (400) 'notjson'", error.exception());
  }

  @Test
  public void testGenericImporterUnexpectedResponseCode() throws Exception {
    InMemoryIdempotentImportExecutor executor = new InMemoryIdempotentImportExecutor(monitor);
    GenericImporter<IdOnlyContainerResource, String> importer =
        getImporter(
            importerClass,
            container ->
                    List.of(
                            new ImportableData<>(
                                    new GenericPayload<>(container.getId(), "schemasource"),
                                    container.getId(),
                                    container.getId())));
    webServer.enqueue(new MockResponse().setResponseCode(111));

    importer.importItem(
        UUID.randomUUID(),
        executor,
        new TokensAndUrlAuthData(
            "accessToken", "refreshToken", webServer.url("/refresh").toString()),
        new IdOnlyContainerResource("itemId"));

    Collection<ErrorDetail> errors = executor.getErrors();
    assertEquals(1, errors.size());
    ErrorDetail error = errors.iterator().next();
    assertEquals("itemId", error.title());
    assertContains("Unexpected response code (111)", error.exception());
  }

  @Test
  public void testGenericImporterDestinationFull() throws Exception {
    InMemoryIdempotentImportExecutor executor = new InMemoryIdempotentImportExecutor(monitor);
    GenericImporter<IdOnlyContainerResource, String> importer =
            getImporter(
                    importerClass,
                    container ->
                            List.of(
                                    new ImportableData<>(
                                            new GenericPayload<>(container.getId(), "schemasource"),
                                            container.getId(),
                                            container.getId())));
    webServer.enqueue(new MockResponse().setResponseCode(413).setBody("{\"error\":\"destination_full\"}"));

    assertThrows(DestinationMemoryFullException.class, () -> {
      importer.importItem(
              UUID.randomUUID(),
              executor,
              new TokensAndUrlAuthData(
                      "accessToken", "refreshToken", webServer.url("/refresh").toString()),
              new IdOnlyContainerResource("itemId"));
    });


    Collection<ErrorDetail> errors = executor.getErrors();
    assertEquals(1, errors.size());
    ErrorDetail error = errors.iterator().next();
    assertEquals("itemId", error.title());
    assertContains("Generic importer failed with code (413)", error.exception());
  }
}
