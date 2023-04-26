/*
 * Copyright 2023 The Data Transfer Project Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.datatransferproject.datatransfer.apple.photos.streaming;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.datatransfer.apple.constants.ApplePhotosConstants;
import org.datatransferproject.datatransfer.apple.exceptions.AppleContentException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * An Http Client to handle uploading and downloading of the streaming content.
 */
public class StreamingContentClient {
  private HttpURLConnection connection;
  private DataOutputStream outputStream;

  private Monitor monitor;

  public enum StreamingMode {
    UPLOAD,
    DOWNLOAD
  };

  /**
   * Creates a streaming session with the specified url
   *
   * @param url the url to upload or download from
   * @param mode indicates if this is an upload or a download session
   * @throws IOException
   */
  public StreamingContentClient(
      @NotNull final String url, @NotNull final StreamingMode mode, @NotNull Monitor monitor)
      throws IOException {
    this.monitor = monitor;
    URL urlObject = new URL(url);
    connection = (HttpURLConnection) urlObject.openConnection();
    connection.setRequestProperty("Transfer-Encoding", "chunked");
    connection.setRequestProperty("content-type", "application/octet-stream");
    connection.setDoOutput(true);
    if (mode.equals(StreamingMode.UPLOAD)) {
      connection.setDoInput(true);
      connection.setChunkedStreamingMode(ApplePhotosConstants.contentRequestLength);
      connection.setRequestMethod("POST");
      outputStream = new DataOutputStream(connection.getOutputStream());
    } else {
      connection.setRequestMethod("GET");
    }
  }

  /**
   * Uploads the given bytes to the url specified in the constructor. If lastRequest is true, the
   * upload is complete and a ContentResponse is returned. Otherwise, null is returned.
   *
   * @param uploadBytes
   * @return ContentResponse
   * @throws AppleContentException
   */
  @Nullable
  public void uploadBytes(@NotNull final byte[] uploadBytes) throws AppleContentException {
    try {
      outputStream.write(uploadBytes);
    } catch (IOException e) {
      monitor.severe(() -> "Error when uploading to content", e);
      connection.disconnect();
      throw new AppleContentException("Error when uploading to Content", e);
    }
  }

  @Nullable
  public String completeUpload() throws AppleContentException {
    try {
      try {
        StringBuilder content;
        try (BufferedReader br =
            new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
          String line;
          content = new StringBuilder();
          while ((line = br.readLine()) != null) {
            content.append(line);
            content.append(System.lineSeparator());
          }
        }
        return content.toString();
      } finally {
        connection.disconnect();
      }
    } catch (IOException e) {
      monitor.severe(() -> "Error when completing upload", e);
      connection.disconnect();
      throw new AppleContentException("Error when uploading to Content", e);
    }
  }

  /**
   * Attempts to read the given number of bytes from the url specified in the constructor. If less
   * than the given number of bytes are available, return a truncated buffer. If no bytes are
   * available, return null.
   *
   * @param maxBytesToRead
   * @return bytes read from url (or null if none can be read)
   * @throws AppleContentException
   */
  @Nullable
  public byte[] downloadBytes(final int maxBytesToRead) throws AppleContentException {
    final byte[] buffer = new byte[maxBytesToRead];

    try {
      int bytesRead = IOUtils.read(connection.getInputStream(), buffer);
      // re-try if a 301 is received, otherwise throw an exception
      if (connection.getResponseCode() != HttpStatus.SC_OK) {
        if (connection.getResponseCode() == HttpStatus.SC_MOVED_PERMANENTLY) {
          final String newUrl = connection.getHeaderField(HttpHeaders.LOCATION);
          URL urlObject = new URL(newUrl);
          connection = (HttpURLConnection) urlObject.openConnection();
          connection.setRequestProperty("Transfer-Encoding", "chunked");
          connection.setRequestProperty("content-type", "application/octet-stream");
          connection.setDoOutput(true);
          connection.setRequestMethod("GET");

          bytesRead = IOUtils.read(connection.getInputStream(), buffer);

          if (connection.getResponseCode() != HttpStatus.SC_OK) {
            throw new IOException(
                "Error response code when trying to download content "
                    + connection.getResponseCode());
          }
        } else {
          throw new IOException(
              "Error response code when trying to download content "
                  + connection.getResponseCode());
        }
      }
      if (bytesRead < maxBytesToRead) {
        connection.disconnect();
        if (bytesRead <= 0) {
          return null;
        } else {
          final byte[] truncatedBuffer = Arrays.copyOf(buffer, bytesRead);
          return truncatedBuffer;
        }
      }
      return buffer;
    } catch (IOException e) {
      monitor.severe(() -> "Error when downloading from Content", e);
      connection.disconnect();
      throw new AppleContentException("Error when downloading from Content", e);
    }
  }
}
