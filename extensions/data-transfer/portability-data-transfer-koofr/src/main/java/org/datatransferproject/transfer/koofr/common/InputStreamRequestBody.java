package org.datatransferproject.transfer.koofr.common;

import java.io.IOException;
import java.io.InputStream;
import javax.annotation.Nullable;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.BufferedSink;
import okio.Okio;
import okio.Source;

// from https://github.com/square/okhttp/issues/3585#issuecomment-327319196
public class InputStreamRequestBody extends RequestBody {
  private final InputStream inputStream;
  private final MediaType contentType;

  public InputStreamRequestBody(MediaType contentType, InputStream inputStream) {
    if (inputStream == null) throw new NullPointerException("inputStream == null");
    this.contentType = contentType;
    this.inputStream = inputStream;
  }

  @Nullable
  @Override
  public MediaType contentType() {
    return contentType;
  }

  @Override
  public long contentLength() throws IOException {
    return -1;
  }

  @Override
  public void writeTo(BufferedSink sink) throws IOException {
    Source source = null;
    try {
      source = Okio.source(inputStream);
      sink.writeAll(source);
    } finally {
      if (source != null) {
        source.close();
      }
    }
  }
}
