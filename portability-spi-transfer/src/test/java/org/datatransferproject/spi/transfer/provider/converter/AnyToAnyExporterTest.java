package org.datatransferproject.spi.transfer.provider.converter;


import static com.google.common.truth.Truth.assertThat;

import java.util.Optional;
import org.datatransferproject.spi.transfer.provider.ExportResult;
import org.datatransferproject.spi.transfer.provider.ExportResult.ResultType;
import org.datatransferproject.spi.transfer.provider.Exporter;
import org.datatransferproject.types.common.ExportInformation;
import org.datatransferproject.types.common.models.DateRangeContainerResource;
import org.datatransferproject.types.common.models.media.MediaContainerResource;
import org.datatransferproject.types.common.models.photos.PhotosContainerResource;
import org.datatransferproject.types.transfer.auth.AuthData;
import org.junit.jupiter.api.Test;

class AnyToAnyExporterTest {

  @Test
  public void shouldHandleConversionFromPhotoExporterToMediaExporterUsingAnyInputContainerResource()
      throws Exception {
    Exporter<AuthData, PhotosContainerResource> photosExporter =
        (jobId, authData, exportInformation) ->
            new ExportResult<>(
                ResultType.END,
                exportInformation
                    .map(ei -> (PhotosContainerResource) ei.getContainerResource())
                    .get());

    AnyToAnyExporter<AuthData, PhotosContainerResource, MediaContainerResource> mediaExporter =
        new AnyToAnyExporter<>(
            photosExporter,
            MediaContainerResource::photoToMedia,
            (cr) -> {
              assertThat(cr).isInstanceOf(DateRangeContainerResource.class);
              return new PhotosContainerResource(null, null);
            });
    ExportInformation ei = new ExportInformation(null, new DateRangeContainerResource(0, 0));

    ExportResult<MediaContainerResource> actual = mediaExporter.export(null, null, Optional.of(ei));

    MediaContainerResource expected = new MediaContainerResource(null, null, null);
    assertThat(actual.getExportedData()).isEqualTo(expected);
  }
}
