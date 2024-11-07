package org.datatransferproject.spi.api.transport;

import java.io.IOException;
import java.io.InputStream;
import org.datatransferproject.types.common.DownloadableItem;

public interface RemoteFileStreamer {
  public InputStream get(String remoteUrl) throws IOException;

  public InputStream get(DownloadableItem downloadableItem) throws IOException;
}
