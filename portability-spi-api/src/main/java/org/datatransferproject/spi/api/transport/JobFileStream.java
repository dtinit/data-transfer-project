package org.datatransferproject.spi.api.transport;

import com.google.common.annotations.VisibleForTesting;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import org.datatransferproject.spi.cloud.storage.TemporaryPerJobDataStore;
import org.datatransferproject.types.common.DownloadableFile;

public class JobFileStream {
  private final RemoteFileStreamer remoteFileStreamer;

  @VisibleForTesting
  public JobFileStream(RemoteFileStreamer remoteFileStreamer) {
    this.remoteFileStreamer = remoteFileStreamer;
  }

  public JobFileStream() {
    this(new UrlGetStreamer());
  }

  /** Streams a file from wherever it lives, making no attempt to tee into jobStore. */
  public InputStream streamFile(
      DownloadableFile downloadableFile, UUID jobId, TemporaryPerJobDataStore jobStore)
      throws IOException {
    if (downloadableFile.isInTempStore()) {
      return jobStore.getStream(jobId, downloadableFile.getFetchableUrl()).getStream();
    } else if (downloadableFile.getFetchableUrl() != null) {
      return this.remoteFileStreamer.get(downloadableFile);
    } else {
      throw new IllegalStateException(
          String.format("jobId %s has no methods to fetch file: %s", jobId, downloadableFile));
    }
  }
}
