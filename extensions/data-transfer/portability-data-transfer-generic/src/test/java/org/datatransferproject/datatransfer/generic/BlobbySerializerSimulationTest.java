package org.datatransferproject.datatransfer.generic;

import static org.junit.Assert.assertEquals;

import com.google.common.collect.Lists;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.cloud.local.LocalJobStore;
import org.datatransferproject.datatransfer.generic.BlobbySerializer.ExportData;
import org.datatransferproject.spi.cloud.storage.TemporaryPerJobDataStore;
import org.datatransferproject.transfer.JobMetadata;
import org.datatransferproject.types.common.models.blob.BlobbyStorageContainerResource;
import org.datatransferproject.types.common.models.blob.DigitalDocumentWrapper;
import org.datatransferproject.types.common.models.blob.DtpDigitalDocument;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class BlobbySerializerSimulationTest {

  private final Monitor monitor = new Monitor() {};
  TemporaryPerJobDataStore jobStore = new LocalJobStore(monitor);
  BlobbySerializer blobbySerializer;

  @Before
  public void setup() throws IOException {
    blobbySerializer = new BlobbySerializer(jobStore);
  }

  @Test
  public void simpleImportTest() throws Exception {

    /** Test
     * Root
     *  Folder 1.1
     *    Folder 1.1.1
     *      bar.txt
     *    Folder 1.1.2
     *      bar.txt
     *   Folder 1.2
     *    bar.txt
     *      */
    try (var mockedStatic = Mockito.mockStatic(JobMetadata.class)) {
      mockedStatic.when(JobMetadata::getJobId).thenReturn(UUID.randomUUID());
      runEndToEndTest();
    }
  }

  private void runEndToEndTest() throws IOException {
    DigitalDocumentWrapper wrapper =
        new DigitalDocumentWrapper(
            new DtpDigitalDocument("bar.txt", null, "text/plain"), "text/plain", "bartxt");

    BlobbyStorageContainerResource rootPre =
        new BlobbyStorageContainerResource(
            "Test Root", "root", Collections.emptyList(), Collections.emptyList());

    BlobbyStorageContainerResource root =
        new BlobbyStorageContainerResource(
            "Test Root",
            "root",
            Collections.emptyList(),
            List.of(
                new BlobbyStorageContainerResource(
                    "Folder 1.1", "1.1", Collections.emptyList(), Collections.emptyList()),
                new BlobbyStorageContainerResource(
                    "Folder 1.2", "1.2", Collections.emptyList(), Collections.emptyList())));

    BlobbyStorageContainerResource folder1_1 =
        new BlobbyStorageContainerResource(
            "Folder 1.1",
            "1.1",
            Collections.emptyList(),
            List.of(
                new BlobbyStorageContainerResource(
                    "Folder 1.1.1", "1.1.1", Collections.emptyList(), Collections.emptyList()),
                new BlobbyStorageContainerResource(
                    "Folder 1.1.2", "1.1.2", Collections.emptyList(), Collections.emptyList())));

    BlobbyStorageContainerResource folder1_2 =
        new BlobbyStorageContainerResource(
            "Folder 1.2", "1.2", List.of(wrapper), Collections.emptyList());

    BlobbyStorageContainerResource folder1_1_1 =
        new BlobbyStorageContainerResource(
            "Folder 1.1.1", "1.1.1", List.of(wrapper), Collections.emptyList());

    BlobbyStorageContainerResource folder1_1_2 =
        new BlobbyStorageContainerResource(
            "Folder 1.1.2", "1.1.2", List.of(wrapper), Collections.emptyList());

    List<ImportableData<ExportData>> data = Lists.newArrayList(blobbySerializer.serialize(rootPre));
    assertEquals(1, data.size());
    assertEquals("/Test Root", data.get(0).getName());

    data = Lists.newArrayList(blobbySerializer.serialize(root));
    assertEquals(3, data.size());
    assertEquals("/Test Root", data.get(0).getName());
    assertEquals("/Test Root/Folder 1.1", data.get(1).getName());
    assertEquals("/Test Root/Folder 1.2", data.get(2).getName());

    data = Lists.newArrayList(blobbySerializer.serialize(folder1_1));
    assertEquals(3, data.size());
    assertEquals("/Test Root/Folder 1.1", data.get(0).getName());
    assertEquals("/Test Root/Folder 1.1/Folder 1.1.1", data.get(1).getName());
    assertEquals("/Test Root/Folder 1.1/Folder 1.1.2", data.get(2).getName());

    data = Lists.newArrayList(blobbySerializer.serialize(folder1_1_1));
    assertEquals(2, data.size());
    assertEquals("/Test Root/Folder 1.1/Folder 1.1.1", data.get(0).getName());
    assertEquals("bar.txt", data.get(1).getName());
    ImportableFileData fileExportData = (ImportableFileData) data.get(1);
    assertEquals(
        ((FileExportData) fileExportData.getJsonData().getPayload()).getFolder(),
        "/Test Root/Folder 1.1/Folder 1.1.1");

    data = Lists.newArrayList(blobbySerializer.serialize(folder1_1_2));
    assertEquals(2, data.size());
    assertEquals("/Test Root/Folder 1.1/Folder 1.1.2", data.get(0).getName());
    assertEquals("bar.txt", data.get(1).getName());
    fileExportData = (ImportableFileData) data.get(1);
    assertEquals(
        ((FileExportData) fileExportData.getJsonData().getPayload()).getFolder(),
        "/Test Root/Folder 1.1/Folder 1.1.2");

    data = Lists.newArrayList(blobbySerializer.serialize(folder1_2));
    assertEquals(2, data.size());
    assertEquals("/Test Root/Folder 1.2", data.get(0).getName());
    assertEquals("bar.txt", data.get(1).getName());
    fileExportData = (ImportableFileData) data.get(1);
    assertEquals(
        ((FileExportData) fileExportData.getJsonData().getPayload()).getFolder(),
        "/Test Root/Folder 1.2");
  }
}
