/*
 * Copyright 2019 The Data Transfer Project Authors.
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

package org.datatransferproject.transfer.microsoft.photos;

import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import com.google.api.client.auth.oauth2.Credential;

import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.spi.transfer.idempotentexecutor.IdempotentImportExecutor;
import org.datatransferproject.launcher.monitor.ConsoleMonitor;
import org.datatransferproject.spi.cloud.storage.TemporaryPerJobDataStore;
import org.datatransferproject.spi.transfer.provider.ImportResult;
import org.datatransferproject.spi.transfer.types.ContinuationData;
import org.datatransferproject.transfer.microsoft.common.MicrosoftCredentialFactory;
import org.datatransferproject.transfer.microsoft.driveModels.*;
import org.datatransferproject.types.common.StringPaginationToken;
import org.datatransferproject.types.common.models.ContainerResource;
import org.datatransferproject.types.common.models.IdOnlyContainerResource;
import org.datatransferproject.types.common.models.photos.PhotoAlbum;
import org.datatransferproject.types.common.models.photos.PhotoModel;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;
import org.datatransferproject.types.common.models.photos.PhotosContainerResource;
import org.datatransferproject.transfer.microsoft.DataChunk;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import com.fasterxml.jackson.databind.DeserializationFeature;


import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


/** */
public class DataChunkTest {

  private static final int CHUNK_SIZE = 32000 * 1024; // 32000KiB

  InputStream inputStream;

  @Before
  public void setUp() throws IOException {
  }

  @Test
  public void testSplitDataSingleFullChunk() throws IOException {
    inputStream = new ByteArrayInputStream(new byte[CHUNK_SIZE]);
    List<DataChunk> l = DataChunk.splitData(inputStream);
    assertThat(l).hasSize(1);
    assertThat(l.get(0).getSize()).isEqualTo(CHUNK_SIZE);
    assertThat(l.get(0).getStart()).isEqualTo(0);
    assertThat(l.get(0).getEnd()).isEqualTo(CHUNK_SIZE - 1);
  }

  @Test
  public void testSplitDataSingleNotFullChunk() throws IOException {
    inputStream = new ByteArrayInputStream(new byte[CHUNK_SIZE-1]);
    List<DataChunk> l = DataChunk.splitData(inputStream);
    assertThat(l).hasSize(1);
    assertThat(l.get(0).getSize()).isEqualTo(CHUNK_SIZE - 1);
    assertThat(l.get(0).getStart()).isEqualTo(0);
    assertThat(l.get(0).getEnd()).isEqualTo(CHUNK_SIZE - 2);
  }

  @Test
  public void testSplitDataEmpty() throws IOException {
    inputStream = new ByteArrayInputStream(new byte[0]);
    List<DataChunk> l = DataChunk.splitData(inputStream);
    assertThat(l).hasSize(0);
  }

  @Test
  public void testSplitTwoEvenChunks() throws IOException {
    inputStream = new ByteArrayInputStream(new byte[CHUNK_SIZE*2]);
    List<DataChunk> l = DataChunk.splitData(inputStream);
    assertThat(l).hasSize(2);
    assertThat(l.get(0).getSize()).isEqualTo(CHUNK_SIZE);
    assertThat(l.get(0).getStart()).isEqualTo(0);
    assertThat(l.get(0).getEnd()).isEqualTo(CHUNK_SIZE - 1);
    assertThat(l.get(1).getSize()).isEqualTo(CHUNK_SIZE);
    assertThat(l.get(1).getStart()).isEqualTo(CHUNK_SIZE);
    assertThat(l.get(1).getEnd()).isEqualTo(2*CHUNK_SIZE - 1);
  }

  @Test
  public void testSplitTwoChunksUneven() throws IOException {
    inputStream = new ByteArrayInputStream(new byte[CHUNK_SIZE*2 - 10]);
    List<DataChunk> l = DataChunk.splitData(inputStream);
    assertThat(l).hasSize(2);
    assertThat(l.get(0).getSize()).isEqualTo(CHUNK_SIZE);
    assertThat(l.get(0).getStart()).isEqualTo(0);
    assertThat(l.get(0).getEnd()).isEqualTo(CHUNK_SIZE - 1);
    assertThat(l.get(1).getSize()).isEqualTo(CHUNK_SIZE - 10);
    assertThat(l.get(1).getStart()).isEqualTo(CHUNK_SIZE);
    assertThat(l.get(1).getEnd()).isEqualTo(2*CHUNK_SIZE - 11);
  }

}
