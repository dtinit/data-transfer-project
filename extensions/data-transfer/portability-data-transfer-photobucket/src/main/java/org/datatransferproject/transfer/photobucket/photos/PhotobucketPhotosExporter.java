package org.datatransferproject.transfer.photobucket.photos;

import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.spi.transfer.provider.ExportResult;
import org.datatransferproject.spi.transfer.provider.Exporter;
import org.datatransferproject.transfer.photobucket.client.PhotobucketCredentialsFactory;
import org.datatransferproject.types.common.ExportInformation;
import org.datatransferproject.types.common.models.photos.PhotosContainerResource;
import org.datatransferproject.types.transfer.auth.AuthData;

import java.util.Optional;
import java.util.UUID;

public class PhotobucketPhotosExporter implements Exporter<AuthData, PhotosContainerResource> {
  private final Monitor monitor;

  public PhotobucketPhotosExporter(PhotobucketCredentialsFactory credentialsFactory, Monitor monitor) {
    this.monitor = monitor;
  }

  @Override
  public ExportResult<PhotosContainerResource> export(
      UUID jobId, AuthData authData, Optional<ExportInformation> exportInformation)
      throws Exception {
    monitor.severe(
        () -> "PhotobucketPhotosExporter is not implemented yet, unable to export data.");
    return null;
  }
}
