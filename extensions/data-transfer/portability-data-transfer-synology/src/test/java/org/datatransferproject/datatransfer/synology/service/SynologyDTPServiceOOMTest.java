/*
 * Copyright 2025 The Data Transfer Project Authors.
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
 *
 */

package org.datatransferproject.datatransfer.synology.service;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.UUID;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okio.Buffer;
import okio.Okio;
import okio.Sink;
import okio.Timeout;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.datatransfer.synology.utils.TestConfigs;
import org.datatransferproject.spi.cloud.storage.JobStore;
import org.datatransferproject.spi.cloud.storage.TemporaryPerJobDataStore.InputStreamWrapper;
import org.datatransferproject.spi.transfer.types.InvalidTokenException;
import org.datatransferproject.types.common.models.photos.PhotoModel;
import org.datatransferproject.types.common.models.videos.VideoModel;
import org.datatransferproject.types.transfer.serviceconfig.TransferServiceConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SynologyDTPServiceOOMTest {
  private final String exportingService = "mockService";
  private final UUID jobId = UUID.randomUUID();
  private SynologyDTPService dtpService;
  @Mock private Monitor monitor;
  @Mock private TransferServiceConfig transferServiceConfig;
  @Mock private JobStore jobStore;
  @Mock private SynologyOAuthTokenManager tokenManager;
  @Captor private ArgumentCaptor<SynologyDTPService.RequestBodyGenerator> requestBodyCaptor;
  @Mock private OkHttpClient client;

  // Helper class for OOM test
  private static class FakeLargeInputStream extends InputStream {
    private final long size;
    private long bytesRead = 0;
    private final byte[] singleByte = new byte[] {'a'};

    public FakeLargeInputStream(long size) {
      this.size = size;
    }

    @Override
    public int read() {
      if (bytesRead >= size) {
        return -1; // End of stream
      }
      bytesRead++;
      return singleByte[0];
    }

    @Override
    public int read(byte[] b, int off, int len) {
      if (b == null) {
        throw new NullPointerException();
      } else if (off < 0 || len < 0 || len > b.length - off) {
        throw new IndexOutOfBoundsException();
      } else if (len == 0) {
        return 0;
      }

      if (bytesRead >= size) {
        return -1;
      }
      long remaining = size - bytesRead;
      int toRead = (int) Math.min(len, remaining);

      // Don't bother filling the array, we are just simulating reading
      // Arrays.fill(b, off, off + toRead, singleByte[0]);

      bytesRead += toRead;
      return toRead;
    }

    @Override
    public int available() {
      long remaining = size - bytesRead;
      return remaining > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) remaining;
    }
  }

  private static class BlackholeSink implements Sink {
    @Override
    public void write(Buffer source, long byteCount) throws IOException {
      source.skip(byteCount);
    }

    @Override
    public void flush() throws IOException {}

    @Override
    public Timeout timeout() {
      return Timeout.NONE;
    }

    @Override
    public void close() throws IOException {}
  }

  @BeforeEach
  public void setUp() throws InvalidTokenException {
    when(transferServiceConfig.getServiceConfig())
        .thenReturn(TestConfigs.createServiceConfigJson());
    dtpService =
        new SynologyDTPService(
            monitor, transferServiceConfig, exportingService, jobStore, tokenManager, client);
  }

  @Test
  public void createPhoto_withLargeFile_shouldStreamData() throws Exception {
    // setup
    long oneGB = 1024L * 1024L * 1024L;
    InputStream fakeInputStream = new FakeLargeInputStream(oneGB);
    InputStreamWrapper streamWrapper = new InputStreamWrapper(fakeInputStream, oneGB);

    PhotoModel photo =
        new PhotoModel(
            "large-photo", "large-photo-url", "desc", "image/jpeg", "photo-id", null, true);

    when(jobStore.getStream(jobId, "large-photo-url")).thenReturn(streamWrapper);

    SynologyDTPService spyService = Mockito.spy(dtpService);
    doReturn(Map.of("success", true, "data", Map.of("item_id", "photo-id")))
        .when(spyService)
        .sendPostRequest(anyString(), requestBodyCaptor.capture(), any());

    // act
    spyService.createPhoto(photo, jobId);

    // Get the generated request body
    RequestBody requestBody = requestBodyCaptor.getValue().get();

    // assert
    // The content length is not asserted to be equal to 1GB because it includes multipart
    // boundaries and headers,
    // making it larger than the raw file size. The main purpose of this test is to ensure that
    // the file is streamed without causing an OutOfMemoryError.
    assertTrue(requestBody.contentLength() > oneGB);

    // Simulate writing the body to a sink that discards the data.
    // This will throw OutOfMemoryError if the whole stream is loaded into memory.
    okio.BufferedSink discardingSink = Okio.buffer(new BlackholeSink());

    // The MultipartBody will try to read the stream to write it.
    // If it buffers the whole 1GB in memory, this will OOM.
    requestBody.writeTo(discardingSink);
    discardingSink.flush();

    // If we reach here, it means we streamed the data without OOM.
  }

  @Test
  public void createVideo_withLargeFile_shouldStreamData() throws Exception {
    // setup
    long oneGB = 1024L * 1024L * 1024L;
    InputStream fakeInputStream = new FakeLargeInputStream(oneGB);
    InputStreamWrapper streamWrapper = new InputStreamWrapper(fakeInputStream, oneGB);

    VideoModel video =
        new VideoModel(
            "large-video", "large-video-url", "desc", "video/mp4", "video-id", null, true, null);

    when(jobStore.getStream(jobId, "large-video-url")).thenReturn(streamWrapper);

    SynologyDTPService spyService = Mockito.spy(dtpService);
    doReturn(Map.of("success", true, "data", Map.of("item_id", "video-id")))
        .when(spyService)
        .sendPostRequest(anyString(), requestBodyCaptor.capture(), any(), anyInt());

    // act
    spyService.createVideo(video, jobId);

    // Get the generated request body
    RequestBody requestBody = requestBodyCaptor.getValue().get();

    // assert
    // The content length is not asserted to be equal to 1GB because it includes multipart
    // boundaries and headers,
    // making it larger than the raw file size. The main purpose of this test is to ensure that
    // the file is streamed without causing an OutOfMemoryError.
    assertTrue(requestBody.contentLength() > oneGB);

    // Simulate writing the body to a sink that discards the data.
    // This will throw OutOfMemoryError if the whole stream is loaded into memory.
    okio.BufferedSink discardingSink = Okio.buffer(new BlackholeSink());

    // The MultipartBody will try to read the stream to write it.
    // If it buffers the whole 1GB in memory, this will OOM.
    requestBody.writeTo(discardingSink);
    discardingSink.flush();

    // If we reach here, it means we streamed the data without OOM.
  }
}
