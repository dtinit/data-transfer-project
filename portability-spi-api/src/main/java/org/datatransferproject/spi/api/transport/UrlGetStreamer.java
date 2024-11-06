package org.datatransferproject.spi.api.transport;

import static com.google.common.base.Preconditions.checkState;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import org.datatransferproject.types.common.DownloadableItem;

/** Implements a simple HTTP GET against a URL to stream the results. */
/* DO NOT MERGE - update Smugmug's codebase to implement their own, since they have special
 * getImageAsStream impl that isn't a simple GET: https://github.com/dtinit/data-transfer-project/blob/9723399b5b4a66ab431822b2a95f45e6d3380b32/extensions/data-transfer/portability-data-transfer-smugmug/src/main/java/org/datatransferproject/transfer/smugmug/photos/SmugMugPhotosImporter.java#L136 */
public class UrlGetStreamer implements RemoteFileStreamer {
  public InputStream get(String remoteUrl) throws IOException {
    return new BufferedInputStream(new URL(remoteUrl).openStream());
  }

  /* DO NOT MERGE- replace current usages (like Msofts's another others) with this. */
  /* DO NOT MERGE- maybe this belongs on RemoteFileStreamer too? (just not with the default
   * impl? */

  /* DO NOT MERGE - delete Flickr's test-double interface in favor of this
   * https://github.com/dtinit/data-transfer-project/blob/9723399b5b4a66ab431822b2a95f45e6d3380b32/extensions/data-transfer/portability-data-transfer-flickr/src/main/java/org/datatransferproject/datatransfer/flickr/media/FlickrMediaImporter.java#L328
   * */
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
}
