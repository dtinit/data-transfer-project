package org.datatransferproject.transfer.koofr.common;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.Credential;
import com.google.common.collect.ImmutableList;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.apache.commons.io.IOUtils;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.spi.transfer.types.DestinationMemoryFullException;
import org.datatransferproject.spi.transfer.types.InvalidTokenException;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class KoofrClientTest {

  private MockWebServer server;
  private KoofrClient client;
  private KoofrCredentialFactory credentialFactory;
  private Monitor monitor;
  private OkHttpClient httpClient;
  private ObjectMapper mapper;
  private Credential credential;
  private TokensAndUrlAuthData authData;

  @Before
  public void setUp() throws IOException {
    server = new MockWebServer();
    server.start();

    httpClient = new OkHttpClient.Builder().build();
    mapper = new ObjectMapper();
    monitor = mock(Monitor.class);
    credentialFactory = mock(KoofrCredentialFactory.class);

    credential = new Credential.Builder(BearerToken.authorizationHeaderAccessMethod()).build();
    credential.setAccessToken("acc");
    credential.setExpirationTimeMilliseconds(null);
    when(credentialFactory.createCredential(any())).thenReturn(credential);

    client =
        new KoofrClient(
            server.url("").toString(), httpClient, httpClient, mapper, monitor, credentialFactory);

    authData = new TokensAndUrlAuthData("acc", "refresh", "");
    client.getOrCreateCredential(authData);
  }

  @After
  public void tearDown() throws Exception {
    server.shutdown();
  }

  @Test
  public void testFileExists() throws Exception {
    server.enqueue(new MockResponse().setResponseCode(200));

    boolean exists = client.fileExists("/path/to/file");

    Assert.assertTrue(exists);
    Assert.assertEquals(1, server.getRequestCount());

    final RecordedRequest recordedRequest = server.takeRequest();

    Assert.assertEquals("GET", recordedRequest.getMethod());
    Assert.assertEquals(
        "/api/v2/mounts/primary/files/info?path=%2Fpath%2Fto%2Ffile", recordedRequest.getPath());
    Assert.assertEquals("Bearer acc", recordedRequest.getHeader("Authorization"));
    Assert.assertEquals("2.1", recordedRequest.getHeader("X-Koofr-Version"));
  }

  @Test
  public void testFileExistsNonExistent() throws Exception {
    server.enqueue(new MockResponse().setResponseCode(404));

    boolean exists = client.fileExists("/path/to/file");

    Assert.assertFalse(exists);
    Assert.assertEquals(1, server.getRequestCount());

    final RecordedRequest recordedRequest = server.takeRequest();

    Assert.assertEquals("GET", recordedRequest.getMethod());
    Assert.assertEquals(
        "/api/v2/mounts/primary/files/info?path=%2Fpath%2Fto%2Ffile", recordedRequest.getPath());
    Assert.assertEquals("Bearer acc", recordedRequest.getHeader("Authorization"));
    Assert.assertEquals("2.1", recordedRequest.getHeader("X-Koofr-Version"));
  }

  @Test
  public void testFileExistsTokenExpired() throws Exception {
    when(credentialFactory.refreshCredential(credential))
        .then(
            (InvocationOnMock invocation) -> {
              final Credential cred = invocation.getArgument(0);
              cred.setAccessToken("acc1");
              return cred;
            });

    server.enqueue(new MockResponse().setResponseCode(401));
    server.enqueue(new MockResponse().setResponseCode(200));

    boolean exists = client.fileExists("/path/to/file");

    Assert.assertTrue(exists);
    Assert.assertEquals(2, server.getRequestCount());

    RecordedRequest recordedRequest = server.takeRequest();

    Assert.assertEquals("GET", recordedRequest.getMethod());
    Assert.assertEquals(
        "/api/v2/mounts/primary/files/info?path=%2Fpath%2Fto%2Ffile", recordedRequest.getPath());
    Assert.assertEquals("Bearer acc", recordedRequest.getHeader("Authorization"));
    Assert.assertEquals("2.1", recordedRequest.getHeader("X-Koofr-Version"));

    recordedRequest = server.takeRequest();

    Assert.assertEquals("GET", recordedRequest.getMethod());
    Assert.assertEquals(
        "/api/v2/mounts/primary/files/info?path=%2Fpath%2Fto%2Ffile", recordedRequest.getPath());
    Assert.assertEquals("Bearer acc1", recordedRequest.getHeader("Authorization"));
    Assert.assertEquals("2.1", recordedRequest.getHeader("X-Koofr-Version"));
  }

  @Test
  public void testFileExistsRefreshTokenNotFound() throws Exception {
    when(credentialFactory.refreshCredential(credential))
        .then(
            (InvocationOnMock invocation) -> {
              throw new InvalidTokenException("Unable to refresh token.", null);
            });

    server.enqueue(new MockResponse().setResponseCode(401));

    InvalidTokenException caughtExc = null;

    try {
      client.fileExists("/path/to/file");
    } catch (InvalidTokenException exc) {
      caughtExc = exc;
    }

    Assert.assertNotNull(caughtExc);
    Assert.assertEquals("Unable to refresh token.", caughtExc.getMessage());

    Assert.assertEquals(1, server.getRequestCount());

    RecordedRequest recordedRequest = server.takeRequest();

    Assert.assertEquals("GET", recordedRequest.getMethod());
    Assert.assertEquals(
        "/api/v2/mounts/primary/files/info?path=%2Fpath%2Fto%2Ffile", recordedRequest.getPath());
    Assert.assertEquals("Bearer acc", recordedRequest.getHeader("Authorization"));
    Assert.assertEquals("2.1", recordedRequest.getHeader("X-Koofr-Version"));
  }

  @Test
  public void testFileExistsError() throws Exception {
    server.enqueue(new MockResponse().setResponseCode(500).setBody("Internal error"));

    Exception caughtExc = null;

    try {
      client.fileExists("/path/to/file");
    } catch (Exception exc) {
      caughtExc = exc;
    }

    Assert.assertNotNull(caughtExc);
    Assert.assertEquals(
        "Got error code: 500 message: Server Error body: Internal error", caughtExc.getMessage());

    Assert.assertEquals(1, server.getRequestCount());
  }

  @Test
  public void testEnsureFolder() throws Exception {
    server.enqueue(new MockResponse().setResponseCode(200));

    client.ensureFolder("/path/to/folder", "name");

    Assert.assertEquals(1, server.getRequestCount());

    final RecordedRequest recordedRequest = server.takeRequest();

    Assert.assertEquals("POST", recordedRequest.getMethod());
    Assert.assertEquals(
        "/api/v2/mounts/primary/files/folder?path=%2Fpath%2Fto%2Ffolder",
        recordedRequest.getPath());
    Assert.assertEquals("Bearer acc", recordedRequest.getHeader("Authorization"));
    Assert.assertEquals("2.1", recordedRequest.getHeader("X-Koofr-Version"));
    Assert.assertEquals(
        "application/json; charset=utf-8", recordedRequest.getHeader("Content-Type"));
    Assert.assertEquals("{\"name\":\"name\"}", recordedRequest.getBody().readUtf8());
  }

  @Test
  public void testEnsureFolderAlreadyExists() throws Exception {
    server.enqueue(new MockResponse().setResponseCode(409));

    client.ensureFolder("/path/to/folder", "name");

    Assert.assertEquals(1, server.getRequestCount());

    final RecordedRequest recordedRequest = server.takeRequest();

    Assert.assertEquals("POST", recordedRequest.getMethod());
    Assert.assertEquals(
        "/api/v2/mounts/primary/files/folder?path=%2Fpath%2Fto%2Ffolder",
        recordedRequest.getPath());
    Assert.assertEquals("Bearer acc", recordedRequest.getHeader("Authorization"));
    Assert.assertEquals("2.1", recordedRequest.getHeader("X-Koofr-Version"));
    Assert.assertEquals(
        "application/json; charset=utf-8", recordedRequest.getHeader("Content-Type"));
    Assert.assertEquals("{\"name\":\"name\"}", recordedRequest.getBody().readUtf8());
  }

  @Test
  public void testEnsureFolderTokenExpired() throws Exception {
    when(credentialFactory.refreshCredential(credential))
        .then(
            (InvocationOnMock invocation) -> {
              final Credential cred = invocation.getArgument(0);
              cred.setAccessToken("acc1");
              return cred;
            });

    server.enqueue(new MockResponse().setResponseCode(401));
    server.enqueue(new MockResponse().setResponseCode(200));

    client.ensureFolder("/path/to/folder", "name");

    Assert.assertEquals(2, server.getRequestCount());

    RecordedRequest recordedRequest = server.takeRequest();

    Assert.assertEquals("POST", recordedRequest.getMethod());
    Assert.assertEquals(
        "/api/v2/mounts/primary/files/folder?path=%2Fpath%2Fto%2Ffolder",
        recordedRequest.getPath());
    Assert.assertEquals("Bearer acc", recordedRequest.getHeader("Authorization"));
    Assert.assertEquals("2.1", recordedRequest.getHeader("X-Koofr-Version"));
    Assert.assertEquals(
        "application/json; charset=utf-8", recordedRequest.getHeader("Content-Type"));
    Assert.assertEquals("{\"name\":\"name\"}", recordedRequest.getBody().readUtf8());

    recordedRequest = server.takeRequest();

    Assert.assertEquals("POST", recordedRequest.getMethod());
    Assert.assertEquals(
        "/api/v2/mounts/primary/files/folder?path=%2Fpath%2Fto%2Ffolder",
        recordedRequest.getPath());
    Assert.assertEquals("Bearer acc1", recordedRequest.getHeader("Authorization"));
    Assert.assertEquals("2.1", recordedRequest.getHeader("X-Koofr-Version"));
    Assert.assertEquals(
        "application/json; charset=utf-8", recordedRequest.getHeader("Content-Type"));
    Assert.assertEquals("{\"name\":\"name\"}", recordedRequest.getBody().readUtf8());
  }

  @Test
  public void testEnsureFolderError() throws Exception {
    server.enqueue(new MockResponse().setResponseCode(500).setBody("Internal error"));

    Exception caughtExc = null;

    try {
      client.ensureFolder("/path/to/folder", "name");
    } catch (Exception exc) {
      caughtExc = exc;
    }

    Assert.assertNotNull(caughtExc);
    Assert.assertEquals(
        "Got error code: 500 message: Server Error body: Internal error", caughtExc.getMessage());

    Assert.assertEquals(1, server.getRequestCount());
  }

  @Test
  public void testAddDescription() throws Exception {
    server.enqueue(new MockResponse().setResponseCode(200));

    client.addDescription("/path/to/folder", "Test description");

    Assert.assertEquals(1, server.getRequestCount());

    final RecordedRequest recordedRequest = server.takeRequest();

    Assert.assertEquals("POST", recordedRequest.getMethod());
    Assert.assertEquals(
        "/api/v2/mounts/primary/files/tags/add?path=%2Fpath%2Fto%2Ffolder",
        recordedRequest.getPath());
    Assert.assertEquals("Bearer acc", recordedRequest.getHeader("Authorization"));
    Assert.assertEquals("2.1", recordedRequest.getHeader("X-Koofr-Version"));
    Assert.assertEquals(
        "application/json; charset=utf-8", recordedRequest.getHeader("Content-Type"));
    Assert.assertEquals(
        "{\"tags\":{\"description\":[\"Test description\"]}}",
        recordedRequest.getBody().readUtf8());
  }

  @Test
  public void testAddDescriptionTokenExpired() throws Exception {
    when(credentialFactory.refreshCredential(credential))
        .then(
            (InvocationOnMock invocation) -> {
              final Credential cred = invocation.getArgument(0);
              cred.setAccessToken("acc1");
              return cred;
            });

    server.enqueue(new MockResponse().setResponseCode(401));
    server.enqueue(new MockResponse().setResponseCode(200));

    client.addDescription("/path/to/folder", "Test description");

    Assert.assertEquals(2, server.getRequestCount());

    RecordedRequest recordedRequest = server.takeRequest();

    Assert.assertEquals("POST", recordedRequest.getMethod());
    Assert.assertEquals(
        "/api/v2/mounts/primary/files/tags/add?path=%2Fpath%2Fto%2Ffolder",
        recordedRequest.getPath());
    Assert.assertEquals("Bearer acc", recordedRequest.getHeader("Authorization"));
    Assert.assertEquals("2.1", recordedRequest.getHeader("X-Koofr-Version"));
    Assert.assertEquals(
        "application/json; charset=utf-8", recordedRequest.getHeader("Content-Type"));
    Assert.assertEquals(
        "{\"tags\":{\"description\":[\"Test description\"]}}",
        recordedRequest.getBody().readUtf8());

    recordedRequest = server.takeRequest();

    Assert.assertEquals("POST", recordedRequest.getMethod());
    Assert.assertEquals(
        "/api/v2/mounts/primary/files/tags/add?path=%2Fpath%2Fto%2Ffolder",
        recordedRequest.getPath());
    Assert.assertEquals("Bearer acc1", recordedRequest.getHeader("Authorization"));
    Assert.assertEquals("2.1", recordedRequest.getHeader("X-Koofr-Version"));
    Assert.assertEquals(
        "application/json; charset=utf-8", recordedRequest.getHeader("Content-Type"));
    Assert.assertEquals(
        "{\"tags\":{\"description\":[\"Test description\"]}}",
        recordedRequest.getBody().readUtf8());
  }

  @Test
  public void testAddDescriptionError() throws Exception {
    server.enqueue(new MockResponse().setResponseCode(500).setBody("Internal error"));

    Exception caughtExc = null;

    try {
      client.addDescription("/path/to/folder", "Test description");
    } catch (Exception exc) {
      caughtExc = exc;
    }

    Assert.assertNotNull(caughtExc);
    Assert.assertEquals(
        "Got error code: 500 message: Server Error body: Internal error", caughtExc.getMessage());

    Assert.assertEquals(1, server.getRequestCount());
  }

  @Test
  public void testUploadFile() throws Exception {
    server.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "application/json")
            .setBody(
                "{\"name\":\"image.jpg\",\"type\":\"file\",\"modified\":1591868314156,\"size\":5,\"contentType\":\"image/jpeg\",\"hash\":\"d05374dc381d9b52806446a71c8e79b1\",\"tags\":{}}"));

    final InputStream inputStream = new ByteArrayInputStream(new byte[] {0, 1, 2, 3, 4});
    String fullPath =
        client.uploadFile("/path/to/folder", "image.jpg", inputStream, "image/jpeg", null, null);
    Assert.assertEquals("/path/to/folder/image.jpg", fullPath);

    Assert.assertEquals(1, server.getRequestCount());

    final RecordedRequest recordedRequest = server.takeRequest();

    Assert.assertEquals("POST", recordedRequest.getMethod());
    Assert.assertEquals(
        "/content/api/v2/mounts/primary/files/put?path=%2Fpath%2Fto%2Ffolder&filename=image.jpg&autorename=true&info=true",
        recordedRequest.getPath());
    Assert.assertEquals("Bearer acc", recordedRequest.getHeader("Authorization"));
    Assert.assertEquals("2.1", recordedRequest.getHeader("X-Koofr-Version"));
    Assert.assertEquals("image/jpeg", recordedRequest.getHeader("Content-Type"));
    Assert.assertEquals(5, recordedRequest.getBodySize());
  }

  @Test
  public void testUploadFileModified() throws Exception {
    server.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "application/json")
            .setBody(
                "{\"name\":\"image.jpg\",\"type\":\"file\",\"modified\":1591868314156,\"size\":5,\"contentType\":\"image/jpeg\",\"hash\":\"d05374dc381d9b52806446a71c8e79b1\",\"tags\":{}}"));

    final InputStream inputStream = new ByteArrayInputStream(new byte[] {0, 1, 2, 3, 4});
    final Date modified = new Date(1596450052000L);
    String fullPath =
        client.uploadFile(
            "/path/to/folder", "image.jpg", inputStream, "image/jpeg", modified, null);
    Assert.assertEquals("/path/to/folder/image.jpg", fullPath);

    Assert.assertEquals(1, server.getRequestCount());

    final RecordedRequest recordedRequest = server.takeRequest();

    Assert.assertEquals("POST", recordedRequest.getMethod());
    Assert.assertEquals(
        "/content/api/v2/mounts/primary/files/put?path=%2Fpath%2Fto%2Ffolder&filename=image.jpg&autorename=true&info=true&modified=1596450052000",
        recordedRequest.getPath());
    Assert.assertEquals("Bearer acc", recordedRequest.getHeader("Authorization"));
    Assert.assertEquals("2.1", recordedRequest.getHeader("X-Koofr-Version"));
    Assert.assertEquals("image/jpeg", recordedRequest.getHeader("Content-Type"));
    Assert.assertEquals(5, recordedRequest.getBodySize());
  }

  @Test
  public void testUploadFileDescription() throws Exception {
    server.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "application/json")
            .setBody(
                "{\"name\":\"image.jpg\",\"type\":\"file\",\"modified\":1591868314156,\"size\":5,\"contentType\":\"image/jpeg\",\"hash\":\"d05374dc381d9b52806446a71c8e79b1\",\"tags\":{}}"));

    final InputStream inputStream = new ByteArrayInputStream(new byte[] {0, 1, 2, 3, 4});
    String fullPath =
        client.uploadFile(
            "/path/to/folder", "image.jpg", inputStream, "image/jpeg", null, "Test description");
    Assert.assertEquals("/path/to/folder/image.jpg", fullPath);

    Assert.assertEquals(1, server.getRequestCount());

    final RecordedRequest recordedRequest = server.takeRequest();

    Assert.assertEquals("POST", recordedRequest.getMethod());
    Assert.assertEquals(
        "/content/api/v2/mounts/primary/files/put?path=%2Fpath%2Fto%2Ffolder&filename=image.jpg&autorename=true&info=true&tags=description%3DTest+description",
        recordedRequest.getPath());
    Assert.assertEquals("Bearer acc", recordedRequest.getHeader("Authorization"));
    Assert.assertEquals("2.1", recordedRequest.getHeader("X-Koofr-Version"));
    Assert.assertEquals("image/jpeg", recordedRequest.getHeader("Content-Type"));
    Assert.assertEquals(5, recordedRequest.getBodySize());
  }

  @Test
  public void testUploadFileRenamed() throws Exception {
    server.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "application/json")
            .setBody(
                "{\"name\":\"image (1).jpg\",\"type\":\"file\",\"modified\":1591868314156,\"size\":5,\"contentType\":\"image/jpeg\",\"hash\":\"d05374dc381d9b52806446a71c8e79b1\",\"tags\":{}}"));

    final InputStream inputStream = new ByteArrayInputStream(new byte[] {0, 1, 2, 3, 4});
    String fullPath =
        client.uploadFile("/path/to/folder", "image.jpg", inputStream, "image/jpeg", null, null);
    Assert.assertEquals("/path/to/folder/image (1).jpg", fullPath);

    Assert.assertEquals(1, server.getRequestCount());

    final RecordedRequest recordedRequest = server.takeRequest();

    Assert.assertEquals("POST", recordedRequest.getMethod());
    Assert.assertEquals(
        "/content/api/v2/mounts/primary/files/put?path=%2Fpath%2Fto%2Ffolder&filename=image.jpg&autorename=true&info=true",
        recordedRequest.getPath());
    Assert.assertEquals("Bearer acc", recordedRequest.getHeader("Authorization"));
    Assert.assertEquals("2.1", recordedRequest.getHeader("X-Koofr-Version"));
    Assert.assertEquals("image/jpeg", recordedRequest.getHeader("Content-Type"));
    Assert.assertEquals(5, recordedRequest.getBodySize());
  }

  @Test
  public void testUploadFileOutOfSpace() throws Exception {
    server.enqueue(
        new MockResponse()
            .setResponseCode(413)
            .setHeader("Content-Type", "application/json")
            .setBody(
                "{\"error\":{\"code\":\"QuotaExceeded\",\"message\":\"Quota exceeded\"},\"requestId\":\"bad2465e-300e-4079-57ad-46b256e74d21\"}"));

    final InputStream inputStream = new ByteArrayInputStream(new byte[] {0, 1, 2, 3, 4});

    DestinationMemoryFullException caughtExc = null;

    try {
      client.uploadFile("/path/to/folder", "image.jpg", inputStream, "image/jpeg", null, null);
    } catch (DestinationMemoryFullException exc) {
      caughtExc = exc;
    }

    Assert.assertNotNull(caughtExc);
    Assert.assertEquals("Koofr quota exceeded", caughtExc.getMessage());

    Assert.assertEquals(1, server.getRequestCount());

    final RecordedRequest recordedRequest = server.takeRequest();

    Assert.assertEquals("POST", recordedRequest.getMethod());
    Assert.assertEquals(
        "/content/api/v2/mounts/primary/files/put?path=%2Fpath%2Fto%2Ffolder&filename=image.jpg&autorename=true&info=true",
        recordedRequest.getPath());
    Assert.assertEquals("Bearer acc", recordedRequest.getHeader("Authorization"));
    Assert.assertEquals("2.1", recordedRequest.getHeader("X-Koofr-Version"));
    Assert.assertEquals("image/jpeg", recordedRequest.getHeader("Content-Type"));
    Assert.assertEquals(5, recordedRequest.getBodySize());
  }

  @Test
  public void testUploadFileTokenExpired() throws Exception {
    when(credentialFactory.refreshCredential(credential))
        .then(
            (InvocationOnMock invocation) -> {
              final Credential cred = invocation.getArgument(0);
              cred.setAccessToken("acc1");
              return cred;
            });

    server.enqueue(new MockResponse().setResponseCode(401));
    server.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "application/json")
            .setBody(
                "{\"name\":\"image.jpg\",\"type\":\"file\",\"modified\":1591868314156,\"size\":5,\"contentType\":\"image/jpeg\",\"hash\":\"d05374dc381d9b52806446a71c8e79b1\",\"tags\":{}}"));

    final InputStream inputStream = new ByteArrayInputStream(new byte[] {0, 1, 2, 3, 4});
    String fullPath =
        client.uploadFile("/path/to/folder", "image.jpg", inputStream, "image/jpeg", null, null);
    Assert.assertEquals("/path/to/folder/image.jpg", fullPath);

    Assert.assertEquals(2, server.getRequestCount());

    RecordedRequest recordedRequest = server.takeRequest();

    Assert.assertEquals("POST", recordedRequest.getMethod());
    Assert.assertEquals(
        "/content/api/v2/mounts/primary/files/put?path=%2Fpath%2Fto%2Ffolder&filename=image.jpg&autorename=true&info=true",
        recordedRequest.getPath());
    Assert.assertEquals("Bearer acc", recordedRequest.getHeader("Authorization"));
    Assert.assertEquals("2.1", recordedRequest.getHeader("X-Koofr-Version"));
    Assert.assertEquals("image/jpeg", recordedRequest.getHeader("Content-Type"));
    Assert.assertEquals(5, recordedRequest.getBodySize());

    recordedRequest = server.takeRequest();

    Assert.assertEquals("POST", recordedRequest.getMethod());
    Assert.assertEquals(
        "/content/api/v2/mounts/primary/files/put?path=%2Fpath%2Fto%2Ffolder&filename=image.jpg&autorename=true&info=true",
        recordedRequest.getPath());
    Assert.assertEquals("Bearer acc1", recordedRequest.getHeader("Authorization"));
    Assert.assertEquals("2.1", recordedRequest.getHeader("X-Koofr-Version"));
    Assert.assertEquals("image/jpeg", recordedRequest.getHeader("Content-Type"));
    Assert.assertEquals(5, recordedRequest.getBodySize());
  }

  @Test
  public void testUploadFileError() throws Exception {
    server.enqueue(new MockResponse().setResponseCode(500).setBody("Internal error"));

    final InputStream inputStream = new ByteArrayInputStream(new byte[] {0, 1, 2, 3, 4});

    Exception caughtExc = null;

    try {
      client.uploadFile("/path/to/folder", "image.jpg", inputStream, "image/jpeg", null, null);
    } catch (Exception exc) {
      caughtExc = exc;
    }

    Assert.assertNotNull(caughtExc);
    Assert.assertEquals(
        "Got error code: 500 message: Server Error body: Internal error", caughtExc.getMessage());

    Assert.assertEquals(1, server.getRequestCount());
  }

  @Test
  public void testListRecursive() throws Exception {
    final String listRecursiveResponse =
        IOUtils.toString(
            getClass().getClassLoader().getResourceAsStream("listrecursive.jsonl"),
            StandardCharsets.UTF_8);
    server.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "application/x-ndjson; charset=utf-8")
            .setChunkedBody(listRecursiveResponse, 1024));

    List<FilesListRecursiveItem> items = client.listRecursive("/Data transfer");

    Assert.assertEquals(Fixtures.listRecursiveItems, items);
  }

  @Test
  public void testListRecursiveNotFound() throws Exception {
    server.enqueue(
        new MockResponse()
            .setResponseCode(404)
            .setHeader("Content-Type", "application/json")
            .setBody(
                "{\"error\":{\"code\":\"NotFound\",\"message\":\"File not found\"},\"requestId\":\"bad2465e-300e-4079-57ad-46b256e74d21\"}"));

    List<FilesListRecursiveItem> items = client.listRecursive("/Data transfer");

    Assert.assertEquals(ImmutableList.of(), items);
  }

  @Test
  public void testFileLink() throws Exception {
    server.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "application/json")
            .setBody(
                "{\"link\":\"https://app-1.koofr.net/content/files/get/Video+1.mp4?base=TESTBASE\"}"));

    String link = client.fileLink("/Data transfer/Videos/Video 1.mp4");

    Assert.assertEquals(
        "https://app-1.koofr.net/content/files/get/Video+1.mp4?base=TESTBASE", link);
  }

  @Test
  public void testEnsureRootFolder() throws Exception {
    server.enqueue(new MockResponse().setResponseCode(200));

    client.ensureRootFolder();

    Assert.assertEquals(1, server.getRequestCount());

    final RecordedRequest recordedRequest = server.takeRequest();

    Assert.assertEquals("POST", recordedRequest.getMethod());
    Assert.assertEquals("/api/v2/mounts/primary/files/folder?path=%2F", recordedRequest.getPath());
    Assert.assertEquals("Bearer acc", recordedRequest.getHeader("Authorization"));
    Assert.assertEquals("2.1", recordedRequest.getHeader("X-Koofr-Version"));
    Assert.assertEquals(
        "application/json; charset=utf-8", recordedRequest.getHeader("Content-Type"));
    Assert.assertEquals("{\"name\":\"Data transfer\"}", recordedRequest.getBody().readUtf8());

    client.ensureRootFolder();

    Assert.assertEquals(1, server.getRequestCount());
  }

  @Test
  public void testEnsureVideosFolder() throws Exception {
    server.enqueue(new MockResponse().setResponseCode(200));
    client.ensureRootFolder();
    Assert.assertEquals(1, server.getRequestCount());
    server.takeRequest();

    server.enqueue(new MockResponse().setResponseCode(200));

    client.ensureVideosFolder();

    Assert.assertEquals(2, server.getRequestCount());

    final RecordedRequest recordedRequest = server.takeRequest();

    Assert.assertEquals("POST", recordedRequest.getMethod());
    Assert.assertEquals(
        "/api/v2/mounts/primary/files/folder?path=%2FData+transfer", recordedRequest.getPath());
    Assert.assertEquals("Bearer acc", recordedRequest.getHeader("Authorization"));
    Assert.assertEquals("2.1", recordedRequest.getHeader("X-Koofr-Version"));
    Assert.assertEquals(
        "application/json; charset=utf-8", recordedRequest.getHeader("Content-Type"));
    Assert.assertEquals("{\"name\":\"Videos\"}", recordedRequest.getBody().readUtf8());

    client.ensureVideosFolder();

    Assert.assertEquals(2, server.getRequestCount());
  }
}
