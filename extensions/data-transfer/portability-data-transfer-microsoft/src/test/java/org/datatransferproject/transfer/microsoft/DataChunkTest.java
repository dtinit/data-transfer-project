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

import static com.google.common.truth.Truth.assertThat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.datatransferproject.transfer.microsoft.DataChunk;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


/** */
public class DataChunkTest {

  private static final int CHUNK_SIZE = 32000 * 1024; // 32000KiB

  InputStream inputStream;

  @BeforeEach
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
