package org.datatransferproject.transfer.microsoft.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.squareup.okhttp.HttpUrl;
import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.MockWebServer;
import java.util.Optional;
import java.util.UUID;
import okhttp3.OkHttpClient;
import okio.Buffer;
import org.datatransferproject.spi.transfer.provider.ExportResult;
import org.datatransferproject.transfer.microsoft.helper.MockJobStore;
import org.datatransferproject.transfer.microsoft.photos.MicrosoftPhotosExporter;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;
import org.datatransferproject.types.common.models.photos.PhotoAlbum;
import org.datatransferproject.types.common.models.photos.PhotoModel;
import org.datatransferproject.types.common.models.photos.PhotosContainerResource;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 *
 */
public class MicrosoftPhotosExportTest {

  private static final String PHOTOS_RESPONSE = "{\n" +
      "    \"@odata.context\": \"https://graph.microsoft.com/v1.0/$metadata#users('test%40test.com')/drive/special('photos')/children\",\n"
      +
      "    \"value\": [\n" +
      "        {\n" +
      "            \"id\": \"folder1\",\n" +
      "            \"name\": \"Test Picture Folder\",\n" +
      "            \"parentReference\": {\n" +
      "                \"driveId\": \"123\",\n" +
      "                \"driveType\": \"personal\",\n" +
      "                \"id\": \"789\",\n" +
      "                \"name\": \"Pictures\",\n" +
      "                \"path\": \"/drive/root:/Pictures\"\n" +
      "            },\n" +
      "            \"size\": 2298995,\n" +
      "            \"webUrl\": \"https://test.com\",\n" +
      "            \"folder\": {\n" +
      "                \"childCount\": 2,\n" +
      "                \"view\": {\n" +
      "                    \"viewType\": \"thumbnails\",\n" +
      "                    \"sortBy\": \"takenOrCreatedDateTime\",\n" +
      "                    \"sortOrder\": \"ascending\"\n" +
      "                }\n" +
      "            }\n" +
      "        },\n" +
      "        {\n" +
      "            \"id\": \"document1\",\n" +
      "            \"name\": \"Document1.docx\",\n" +
      "            \"parentReference\": {\n" +
      "                \"driveId\": \"123\",\n" +
      "                \"driveType\": \"personal\",\n" +
      "                \"id\": \"789\",\n" +
      "                \"name\": \"Pictures\",\n" +
      "                \"path\": \"/drive/root:/Pictures\"\n" +
      "            },\n" +
      "            \"size\": 9812,\n" +
      "            \"webUrl\": \"https://test.com\",\n" +
      "            \"file\": {\n" +
      "                \"hashes\": {\n" +
      "                    \"sha1Hash\": \"\"\n" +
      "                },\n" +
      "                \"mimeType\": \"application/vnd.openxmlformats-officedocument.wordprocessingml.document\"\n"
      +
      "            }\n" +
      "        }\n" +
      "    ]\n" +
      "}";


  private static final String FOLDER_RESPONSE = "\n" +
      "{\n" +
      "    \"@odata.context\": \"https://graph.microsoft.com/v1.0/$metadata#users(‘test%40test.com')/drive/items(‘folder1’)/children\",\n"
      +
      "    \"value\": [\n" +
      "        {\n" +
      "            \"id\": \"image1\",\n" +
      "            \"name\": \"testimage1.png\",\n" +
      "            \"parentReference\": {\n" +
      "                \"driveId\": \"123\",\n" +
      "                \"driveType\": \"personal\",\n" +
      "                \"id\": \"folder1\",\n" +
      "                \"name\": \"Test Picture Folder\",\n" +
      "                \"path\": \"/drive/root:/Pictures/Test%20Picture%20Folder\"\n" +
      "            },\n" +
      "            \"size\": 785916,\n" +
      "            \"file\": {\n" +
      "                \"hashes\": {\n" +
      "                    \"sha1Hash\": \"1234\"\n" +
      "                },\n" +
      "                \"mimeType\": \"image/png\"\n" +
      "            },\n" +
      "            \"image\": {\n" +
      "                \"height\": 720,\n" +
      "                \"width\": 1280\n" +
      "            },\n" +
      "            \"photo\": {\n" +
      "                \"takenDateTime\": \"2017-10-07T15:42:06.6366667Z\"\n" +
      "            }\n" +
      "        },\n" +
      "        {\n" +
      "            \"id\": \"image2\",\n" +
      "            \"name\": \"testimage2.png\",\n" +
      "            \"parentReference\": {\n" +
      "                \"driveId\": \"123\",\n" +
      "                \"driveType\": \"personal\",\n" +
      "                \"id\": \"folder1\",\n" +
      "                \"name\": \"Test Picture Folder\",\n" +
      "                \"path\": \"/drive/root:/Pictures/Test%20Picture%20Folder\"\n" +
      "            },\n" +
      "            \"size\": 567816,\n" +
      "            \"file\": {\n" +
      "                \"hashes\": {\n" +
      "                    \"sha1Hash\": \"7890\"\n" +
      "                },\n" +
      "                \"mimeType\": \"image/jpeg\"\n" +
      "            },\n" +
      "            \"image\": {\n" +
      "                \"height\": 720,\n" +
      "                \"width\": 1280\n" +
      "            },\n" +
      "            \"photo\": {\n" +
      "                \"takenDateTime\": \"2017-10-07T15:42:06.6366667Z\"\n" +
      "            }\n" +
      "        }\n" +
      " \n" +
      "    ]\n" +
      "}";

  private MockWebServer server;
  private OkHttpClient client;
  private ObjectMapper mapper;
  private TokensAndUrlAuthData token;
  private MockJobStore jobStore;


  @Test
  public void testExport() throws Exception {
    server.enqueue(new MockResponse().setBody(PHOTOS_RESPONSE));
    server.enqueue(new MockResponse().setBody(FOLDER_RESPONSE));

    MockResponse content1Response = createContentResponse("image/png", "Test content1".getBytes());
    server.enqueue(content1Response);

    MockResponse content2Response = createContentResponse("image/jpeg", "Test content2".getBytes());
    server.enqueue(content2Response);

    server.start();

    HttpUrl baseUrl = server.url("");
    MicrosoftPhotosExporter exporter = new MicrosoftPhotosExporter(baseUrl.toString(), client,
        mapper, jobStore);

    ExportResult<PhotosContainerResource> result = exporter
        .export(UUID.randomUUID(), token, Optional.empty());

    PhotosContainerResource resource = result.getExportedData();

    Assert.assertEquals(1, resource.getAlbums().size());
    PhotoAlbum album = resource.getAlbums().iterator().next();
    Assert.assertEquals("folder1", album.getId());
    Assert.assertEquals("Test Picture Folder", album.getName());

    Assert.assertEquals(2, resource.getPhotos().size());

    PhotoModel photo1 = resource.getPhotos().stream().filter(p -> "image1".equals(p.getDataId()))
        .findFirst().get();
    Assert.assertEquals("testimage1.png", photo1.getTitle());
    Assert.assertEquals("image1", photo1.getDataId());
    Assert.assertEquals("image/png", photo1.getMediaType());
    Assert.assertEquals(album.getId(), photo1.getAlbumId());

    PhotoModel photo2 = resource.getPhotos().stream().filter(p -> "image2".equals(p.getDataId()))
        .findFirst().get();
    Assert.assertEquals("testimage2.png", photo2.getTitle());
    Assert.assertEquals("image2", photo2.getDataId());
    Assert.assertEquals("image/jpeg", photo2.getMediaType());
    Assert.assertEquals(album.getId(), photo2.getAlbumId());
  }

  private MockResponse createContentResponse(String contentType, byte[] content) {
    MockResponse response = new MockResponse();
    response.setHeader("content-type", contentType);
    Buffer body = new Buffer();
    body.write(content);
    response.setBody(body);
    return response;
  }


  @Before
  public void setUp() {
    client = new OkHttpClient.Builder().build();
    mapper = new ObjectMapper();
    token = new TokensAndUrlAuthData("token456", "refreshToken", "tokenUrl");
    server = new MockWebServer();
    jobStore = new MockJobStore();
  }

  @After
  public void tearDown() throws Exception {
    server.shutdown();
  }
}
