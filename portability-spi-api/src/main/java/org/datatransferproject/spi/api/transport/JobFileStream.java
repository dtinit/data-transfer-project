package org.datatransferproject.spi.api.transport;

import org.datatransferproject.spi.cloud.storage.TemporaryPerJobDataStore;
import org.datatransferproject.types.common.DownloadableFile;
import java.io.InputStream;
import java.net.URL;

public class JobFileStream() {
  /* DO NOT MERGE - maybe share this afterall, along with DI-ability of URL.openStream() */
  public JobFileStream() {
  }

  /** Streams a file from wherever it lives, making no attempt to tee into jobStore. */
  public InputStream streamFile(DownloadableFile downloadableFile, UUID jobId, TemporaryPerJobDataStore jobStore) {
    if (downloadableFile.isInTempStore()) {
      return jobStore.getStream(jobId, downloadableFile.getFetchableUrl()).getStream();
    } else if (downloadableFile.getFetchableUrl() != null) {
      return new URL(downloadableFile.getFetchableUrl()).openStream();
    } else {
      throw new IllegalStateException(
          String.format("jobid %s has no methods to fetch file: %s", jobId, downloadableFile));
    }
  }
}
