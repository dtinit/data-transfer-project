package org.datatransferproject.spi.transfer.provider;

import static com.google.common.truth.Truth.assertThat;
import static org.datatransferproject.types.common.models.DataVertical.MEDIA;
import static org.datatransferproject.types.common.models.DataVertical.PHOTOS;
import static org.datatransferproject.types.common.models.DataVertical.VIDEOS;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.datatransferproject.spi.transfer.extension.TransferExtension;
import org.datatransferproject.spi.transfer.provider.converter.AnyToAnyExporter;
import org.datatransferproject.spi.transfer.provider.converter.AnyToAnyImporter;
import org.datatransferproject.spi.transfer.provider.converter.MediaExporterDecorator;
import org.datatransferproject.spi.transfer.provider.converter.MediaImporterDecorator;
import org.junit.jupiter.api.Test;

public class TransferCompatibilityProviderTest {

  TransferCompatibilityProvider compatibilityProvider = new TransferCompatibilityProvider();

  @Test
  public void shouldPreferOriginalAdapterWhenAvailable() {
    TransferExtension ext = mock(TransferExtension.class);
    Importer importer = mock(Importer.class);
    when(ext.getImporter(eq(MEDIA))).thenReturn(importer);
    when(ext.getImporter(eq(PHOTOS))).thenReturn(mock(Importer.class));
    when(ext.getImporter(eq(VIDEOS))).thenReturn(mock(Importer.class));
    Exporter exporter = mock(Exporter.class);
    when(ext.getExporter(eq(MEDIA))).thenReturn(exporter);
    when(ext.getExporter(eq(PHOTOS))).thenReturn(mock(Exporter.class));
    when(ext.getExporter(eq(VIDEOS))).thenReturn(mock(Exporter.class));

    assertThat(compatibilityProvider.getCompatibleImporter(ext, MEDIA)).isSameAs(importer);
    assertThat(compatibilityProvider.getCompatibleExporter(ext, MEDIA)).isSameAs(exporter);
  }

  @Test
  public void shouldConstructMediaImporterFromPhotoAndVideo() {
    TransferExtension ext = mock(TransferExtension.class);
    when(ext.getImporter(eq(PHOTOS))).thenReturn(mock(Importer.class));
    when(ext.getImporter(eq(VIDEOS))).thenReturn(mock(Importer.class));
    when(ext.getImporter(eq(MEDIA))).thenReturn(null);

    Importer<?, ?> imp = compatibilityProvider.getCompatibleImporter(ext, MEDIA);
    assertThat(imp).isInstanceOf(MediaImporterDecorator.class);
  }

  @Test
  public void shouldConstructMediaExporterFromPhotoAndVideo() {
    TransferExtension ext = mock(TransferExtension.class);
    when(ext.getExporter(eq(PHOTOS))).thenReturn(mock(Exporter.class));
    when(ext.getExporter(eq(VIDEOS))).thenReturn(mock(Exporter.class));
    when(ext.getExporter(eq(MEDIA))).thenReturn(null);

    Exporter<?, ?> exp = compatibilityProvider.getCompatibleExporter(ext, MEDIA);
    assertThat(exp).isInstanceOf(MediaExporterDecorator.class);
  }

  @Test
  public void shouldConstructPhotoAndVideoExportersFromMedia() {
    TransferExtension ext = mock(TransferExtension.class);
    when(ext.getExporter(eq(MEDIA))).thenReturn(mock(Exporter.class));

    assertThat(compatibilityProvider.getCompatibleExporter(ext, PHOTOS))
        .isInstanceOf(AnyToAnyExporter.class);
    assertThat(compatibilityProvider.getCompatibleExporter(ext, VIDEOS))
        .isInstanceOf(AnyToAnyExporter.class);
  }

  @Test
  public void shouldConstructPhotoAndVideoImportersFromMedia() {
    TransferExtension ext = mock(TransferExtension.class);
    when(ext.getImporter(eq(MEDIA))).thenReturn(mock(Importer.class));

    assertThat(compatibilityProvider.getCompatibleImporter(ext, PHOTOS))
        .isInstanceOf(AnyToAnyImporter.class);
    assertThat(compatibilityProvider.getCompatibleImporter(ext, VIDEOS))
        .isInstanceOf(AnyToAnyImporter.class);
  }

  @Test
  public void shouldMaintainOriginalException() {
    TransferExtension ext = mock(TransferExtension.class);
    when(ext.getExporter(eq(MEDIA))).thenThrow(new RuntimeException());
    assertThrows(Exception.class, () -> compatibilityProvider.getCompatibleExporter(ext, MEDIA));
  }
}
