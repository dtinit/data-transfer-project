package org.datatransferproject.datatransfer.generic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.spi.cloud.storage.JobStore;
import org.datatransferproject.types.common.models.blob.BlobbyStorageContainerResource;
import org.datatransferproject.types.common.models.blob.DigitalDocumentWrapper;
import org.datatransferproject.types.common.models.blob.DtpDigitalDocument;
import org.junit.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class BlobbySerializerTest extends GenericImportSerializerTestBase {
  @Mock
  JobStore jobStore;
  BlobbySerializer blobbySerializer = new BlobbySerializer(jobStore, new Monitor() {});
  @Test
  public void testBlobbySerializerFolders() throws Exception {
    // Folder structure of
    // /root/
    //   /foo/
    BlobbyStorageContainerResource container =
        new BlobbyStorageContainerResource(
            "root",
            "rootdir",
            new ArrayList<>(),
            Arrays.asList(
                new BlobbyStorageContainerResource(
                    "foo", "foodir", new ArrayList<>(), new ArrayList<>())));

    List<ImportableData<BlobbySerializer.ExportData>> res =
        iterableToList(blobbySerializer.serialize(container));

    assertEquals(2, res.size());
    assertEquals("rootdir", res.get(0).getIdempotentId());
    assertEquals("/root", res.get(0).getName());
    assertJsonEquals("{\"@type\": \"Folder\", \"path\": \"/root\"}", res.get(0).getJsonData());

    assertEquals("foodir", res.get(1).getIdempotentId());
    assertEquals("/root/foo", res.get(1).getName());
    assertJsonEquals("{\"@type\": \"Folder\", \"path\": \"/root/foo\"}", res.get(1).getJsonData());
  }

  @Test
  public void testBlobbySerializerFiles() throws Exception {
    // Folder structure of
    // /root
    //   foo.mp4
    //   bar.txt
    BlobbyStorageContainerResource container =
        new BlobbyStorageContainerResource(
            "root",
            "rootdir",
            Arrays.asList(
                new DigitalDocumentWrapper(
                    new DtpDigitalDocument("foo.mp4", "2020-02-01T01:02:03Z", "video/mp4"),
                    "video/mp4",
                    "foomp4"),
                new DigitalDocumentWrapper(
                    new DtpDigitalDocument("bar.txt", null, "text/plain"), "text/plain", "bartxt")),
            new ArrayList<>());

    List<ImportableData<BlobbySerializer.ExportData>> res =
        iterableToList(blobbySerializer.serialize(container));

    assertEquals(3, res.size());

    assertEquals("rootdir", res.get(0).getIdempotentId());
    assertEquals("/root", res.get(0).getName());
    assertJsonEquals(
        "" + "{" + "  \"@type\": \"Folder\"," + "  \"path\": \"/root\"" + "}",
        res.get(0).getJsonData());

    assertEquals("foomp4", res.get(1).getIdempotentId());
    assertEquals("foo.mp4", res.get(1).getName());
    assertJsonEquals(
        ""
            + "{"
            + "  \"@type\": \"File\","
            + "  \"folder\": \"/root\","
            + "  \"name\": \"foo.mp4\","
            + "  \"dateModified\": \"2020-02-01T01:02:03Z\""
            + "}",
        res.get(1).getJsonData());
    assertTrue(res.get(1) instanceof ImportableFileData);
    assertTrue(
        ((ImportableFileData<BlobbySerializer.ExportData>) res.get(1)).getFile().isInTempStore());
    assertEquals(
        "foomp4",
        ((ImportableFileData<BlobbySerializer.ExportData>) res.get(1)).getFile().getFetchableUrl());

    assertEquals("bartxt", res.get(2).getIdempotentId());
    assertEquals("bar.txt", res.get(2).getName());
    assertJsonEquals(
        ""
            + "{"
            + "  \"@type\": \"File\","
            + "  \"folder\": \"/root\","
            + "  \"name\": \"bar.txt\","
            + "  \"dateModified\": null"
            + "}",
        res.get(2).getJsonData());
    assertTrue(res.get(2) instanceof ImportableFileData);
    assertTrue(
        ((ImportableFileData<BlobbySerializer.ExportData>) res.get(2)).getFile().isInTempStore());
    assertEquals(
        "bartxt",
        ((ImportableFileData<BlobbySerializer.ExportData>) res.get(2)).getFile().getFetchableUrl());
  }

  @Test
  public void testBlobbySerializerNested() throws Exception {
    // Folder structure of
    // /root
    //   /foo/
    //     bar.txt
    BlobbyStorageContainerResource container =
        new BlobbyStorageContainerResource(
            "root",
            "rootdir",
            new ArrayList<>(),
            Arrays.asList(
                new BlobbyStorageContainerResource(
                    "foo",
                    "foodir",
                    Arrays.asList(
                        new DigitalDocumentWrapper(
                            new DtpDigitalDocument("bar.txt", "2020-03-01T01:02:03Z", "text/plain"),
                            "text/plain",
                            "bartxt")),
                    new ArrayList<>())));

    List<ImportableData<BlobbySerializer.ExportData>> res =
        iterableToList(blobbySerializer.serialize(container));

    assertEquals(3, res.size());

    assertEquals("rootdir", res.get(0).getIdempotentId());
    assertEquals("/root", res.get(0).getName());
    assertJsonEquals("{\"@type\": \"Folder\", \"path\": \"/root\"}", res.get(0).getJsonData());

    assertEquals("foodir", res.get(1).getIdempotentId());
    assertEquals("/root/foo", res.get(1).getName());
    assertJsonEquals("{\"@type\": \"Folder\", \"path\": \"/root/foo\"}", res.get(1).getJsonData());

    assertEquals("bartxt", res.get(2).getIdempotentId());
    assertEquals("bar.txt", res.get(2).getName());
    assertJsonEquals(
        ""
            + "{"
            + "  \"@type\": \"File\","
            + "  \"folder\": \"/root/foo\","
            + "  \"name\": \"bar.txt\","
            + "  \"dateModified\": \"2020-03-01T01:02:03Z\""
            + "  }"
            + "}",
        res.get(2).getJsonData());
    assertTrue(res.get(2) instanceof ImportableFileData);
    assertTrue(
        ((ImportableFileData<BlobbySerializer.ExportData>) res.get(2)).getFile().isInTempStore());
    assertEquals(
        "bartxt",
        ((ImportableFileData<BlobbySerializer.ExportData>) res.get(2)).getFile().getFetchableUrl());
  }
}
