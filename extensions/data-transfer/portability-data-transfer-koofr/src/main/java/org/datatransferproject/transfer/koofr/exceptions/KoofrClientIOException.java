package org.datatransferproject.transfer.koofr.exceptions;

import okhttp3.Response;
import okhttp3.ResponseBody;

import java.io.IOException;

public class KoofrClientIOException extends IOException {

  private final int httpResponseCode;

  public KoofrClientIOException(Response response) {
    super(
        String.format(
            "Got error code: %s message: %s body: %s",
            response.code(), response.message(), getResponseBody(response)));

    this.httpResponseCode = response.code();
  }

  public int getCode() {
    return httpResponseCode;
  }

  private static String getResponseBody(Response response) {
    try {
      ResponseBody body = response.body();
      return body != null ? body.string() : null;
    } catch (Exception e) {
      return "Failed to get response body";
    }
  }
}
