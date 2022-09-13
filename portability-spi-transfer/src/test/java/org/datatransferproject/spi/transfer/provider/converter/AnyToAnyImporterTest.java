package org.datatransferproject.spi.transfer.provider.converter;


import static com.google.common.truth.Truth.assertThat;

import org.datatransferproject.spi.transfer.provider.ImportResult;
import org.datatransferproject.spi.transfer.provider.Importer;
import org.datatransferproject.types.common.models.media.MediaContainerResource;
import org.datatransferproject.types.common.models.photos.PhotosContainerResource;
import org.datatransferproject.types.transfer.auth.AuthData;
import org.junit.jupiter.api.Test;

class AnyToAnyImporterTest {

  @Test
  public void shouldHandleConversionFromPhotoImporterToMediaImporter()
      throws Exception {
    Importer<AuthData, PhotosContainerResource> photosImporter =
        (jobId, idempotentExecutor, authData, data) -> ImportResult.OK;

    MediaContainerResource mcr = new MediaContainerResource(null, null, null);
    AnyToAnyImporter<AuthData, PhotosContainerResource, MediaContainerResource> mediaExporter =
        new AnyToAnyImporter<>(photosImporter, MediaContainerResource::mediaToPhoto);

    ImportResult res = mediaExporter.importItem(null, null, null, mcr);
    assertThat(res).isEqualTo(ImportResult.OK);
  }
}
