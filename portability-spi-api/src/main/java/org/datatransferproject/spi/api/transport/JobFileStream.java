package org.datatransferproject.spi.api.transport;

import static com.google.common.base.Preconditions.checkState;

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
    checkState(downloadableFile.getFetchableUrl() != null, "missing fetchable URL for file");
    if (downloadableFile.isInTempStore()) {
      return jobStore.getStream(jobId, downloadableFile.getFetchableUrl()).getStream();
    } else {
      return this.remoteFileStreamer.get(downloadableFile);
    }
  }
}
