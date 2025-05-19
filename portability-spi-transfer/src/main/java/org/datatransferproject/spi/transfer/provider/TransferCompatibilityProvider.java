package org.datatransferproject.spi.transfer.provider;

import static org.datatransferproject.types.common.models.DataVertical.MEDIA;
import static org.datatransferproject.types.common.models.DataVertical.PHOTOS;
import static org.datatransferproject.types.common.models.DataVertical.VIDEOS;

import java.util.function.Function;
import org.datatransferproject.types.common.models.DataVertical;
import org.datatransferproject.spi.transfer.extension.TransferExtension;
import org.datatransferproject.spi.transfer.provider.converter.AnyToAnyExporter;
import org.datatransferproject.spi.transfer.provider.converter.AnyToAnyImporter;
import org.datatransferproject.spi.transfer.provider.converter.MediaExporterDecorator;
import org.datatransferproject.spi.transfer.provider.converter.MediaImporterDecorator;
import org.datatransferproject.types.common.models.ContainerResource;
import org.datatransferproject.types.common.models.media.MediaContainerResource;
import org.datatransferproject.types.common.models.photos.PhotosContainerResource;
import org.datatransferproject.types.common.models.videos.VideosContainerResource;

/**
 * Provides a compatibility layer between adapter types. E.g. when asking for a Media adapter and it
 * isn't available, it will combine Photo and Video adapters on the fly to seamlessly support the
 * request.
 */
public class TransferCompatibilityProvider {

  public Exporter getCompatibleExporter(TransferExtension extension, DataVertical jobType) {
    Exporter<?, ?> exporter = getExporterOrNull(extension, jobType);
    if (exporter != null) {
      return exporter;
    }

    switch (jobType) {
      case MEDIA:
        exporter = getMediaExporter(extension);
        break;
      case PHOTOS:
        exporter = getPhotosExporter(extension);
        break;
      case VIDEOS:
        exporter = getVideosExporter(extension);
        break;
    }
    if (exporter == null) {
      return extension.getExporter(jobType); // re-execute just for the potential exception
    }
    return exporter;
  }

  public Importer getCompatibleImporter(TransferExtension extension, DataVertical jobType) {
    Importer<?, ?> importer = getImporterOrNull(extension, jobType);
    if (importer != null) {
      return importer;
    }

    switch (jobType) {
      case MEDIA:
        importer = getMediaImporter(extension);
        break;
      case PHOTOS:
        importer = getPhotosImporter(extension);
        break;
      case VIDEOS:
        importer = getVideosImporter(extension);
        break;
    }
    if (importer == null) {
      return extension.getImporter(jobType); // re-execute just for the potential exception
    }
    return importer;
  }

  private Importer<?, ?> getVideosImporter(TransferExtension extension) {
    Importer mediaImporter = getImporterOrNull(extension, MEDIA);
    if (mediaImporter == null) {
      return null;
    }
    return new AnyToAnyImporter<>(mediaImporter, MediaContainerResource::videoToMedia);
  }

  private Importer<?, ?> getPhotosImporter(TransferExtension extension) {
    Importer mediaImporter = getImporterOrNull(extension, MEDIA);
    if (mediaImporter == null) {
      return null;
    }
    return new AnyToAnyImporter<>(mediaImporter, MediaContainerResource::photoToMedia);
  }

  private Importer<?, ?> getMediaImporter(TransferExtension extension) {
    Importer<?, ?> photo = getImporterOrNull(extension, PHOTOS);
    Importer<?, ?> video = getImporterOrNull(extension, VIDEOS);
    if (photo == null || video == null) {
      return null;
    }
    return new MediaImporterDecorator(photo, video);
  }

  private Exporter<?, ?> getVideosExporter(TransferExtension extension) {
    Exporter media = getExporterOrNull(extension, MEDIA);
    if (media == null) {
      return null;
    }
    Function<ContainerResource, ContainerResource> converter = (cr) ->
        (cr instanceof VideosContainerResource) ?
            MediaContainerResource.videoToMedia((VideosContainerResource) cr) : cr;
    return new AnyToAnyExporter<>(media, MediaContainerResource::mediaToVideo, converter);
  }

  private Exporter<?, ?> getPhotosExporter(TransferExtension extension) {
    Exporter media = getExporterOrNull(extension, MEDIA);
    if (media == null) {
      return null;
    }
    Function<ContainerResource, ContainerResource> converter = (cr) ->
        (cr instanceof PhotosContainerResource) ?
            MediaContainerResource.photoToMedia((PhotosContainerResource) cr) : cr;
    return new AnyToAnyExporter<>(media, MediaContainerResource::mediaToPhoto, converter);
  }

  private Exporter<?, ?> getMediaExporter(TransferExtension extension) {
    Exporter<?, ?> photo = getExporterOrNull(extension, PHOTOS);
    Exporter<?, ?> video = getExporterOrNull(extension, VIDEOS);
    if (photo == null || video == null) {
      return null;
    }
    return new MediaExporterDecorator(photo, video);
  }

  private Exporter<?, ?> getExporterOrNull(TransferExtension extension, DataVertical jobType) {
    // TODO: Don't use exceptions for control flow. Have a way to query supported adapters
    try {
      return extension.getExporter(jobType);
    } catch (Exception e) {
      return null;
    }
  }

  private Importer<?, ?> getImporterOrNull(TransferExtension extension, DataVertical jobType) {
    try {
      return extension.getImporter(jobType);
    } catch (Exception e) {
      return null;
    }
  }
}
