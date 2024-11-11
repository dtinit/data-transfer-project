package org.datatransferproject.spi.api.transport;

import static com.google.common.base.Preconditions.checkState;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import org.datatransferproject.types.common.DownloadableItem;

/** Implements a simple HTTP GET against a URL to stream the results. */
// TODO(techdebt) update Smugmug's codebase to implement their own, since they have special
// getImageAsStream impl that isn't a simple GET:
// https://github.com/dtinit/data-transfer-project/blob/9723399b5b4a66ab431822b2a95f45e6d3380b32/extensions/data-transfer/portability-data-transfer-smugmug/src/main/java/org/datatransferproject/transfer/smugmug/photos/SmugMugPhotosImporter.java#L136
// This will let the DTP codebase share test patterns across adapters.
public class UrlGetStreamer implements RemoteFileStreamer {
  @Override
  public InputStream get(String remoteUrl) throws IOException {
    return new BufferedInputStream(toURL(remoteUrl).openStream());
  }

  @Override
  public InputStream get(DownloadableItem downloadableItem) throws IOException {
    checkState(
        downloadableItem.getFetchableUrl() != null,
        "trying to download incomplete DownloadableItem: missing fetchable URL");
    checkState(
        !downloadableItem.isInTempStore(),
        "trying to re-download an already stored item: \"%s\"",
        downloadableItem.getFetchableUrl());

    return get(downloadableItem.getFetchableUrl());
  }

  /** Easily construct a {@link java.net.URL} while mapping to the exceptions DTP needs. */
  private static URL toURL(String url) throws IOException {
    try {
      return new URI(url).toURL();
    } catch (MalformedURLException | URISyntaxException e) {
      throw new IOException(String.format("invalid URL: \"%s\"", url), e);
    }
  }
}
