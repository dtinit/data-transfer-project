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

package org.datatransferproject.transfer;

import com.google.common.base.Stopwatch;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import org.datatransferproject.api.launcher.DtpInternalMetricRecorder;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.spi.cloud.storage.JobStore;
import org.datatransferproject.spi.transfer.hooks.JobHooks;
import org.datatransferproject.spi.transfer.types.CopyException;
import org.datatransferproject.transfer.copier.InMemoryDataCopier;
import org.datatransferproject.types.common.ExportInformation;
import org.datatransferproject.types.common.models.DataVertical;
import org.datatransferproject.types.transfer.auth.AuthData;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class JobProcessorTest {

  private UUID jobId;
  private ExportInformation exportInfo;
  private AuthData exportAuthData;
  private AuthData importAuthData;
  private InMemoryDataCopier copier;

  private static class TestJobProcessor extends JobProcessor {

    public TestJobProcessor(InMemoryDataCopier copier) {
      super(
          Mockito.mock(JobStore.class),
          Mockito.mock(JobHooks.class),
          null,
          copier,
          null,
          Mockito.mock(Monitor.class),
          Mockito.mock(DtpInternalMetricRecorder.class)
      );
    }
  }

  private TestJobProcessor processor;

  @Before
  public void setUp() {
    importAuthData = exportAuthData = Mockito.mock(AuthData.class);
    jobId = UUID.randomUUID();
    exportInfo = Mockito.mock(ExportInformation.class);
    copier = Mockito.mock(InMemoryDataCopier.class);
    processor = Mockito.spy(new TestJobProcessor(copier));
    JobMetadata.reset();
    JobMetadata.init(
        jobId,
        "".getBytes(),
        DataVertical.BLOBS,
        "",
        "",
        Stopwatch.createStarted());
  }

  @After
  public void cleanUp() {
    JobMetadata.reset();
  }

  @Test
  public void processJobGetsErrorsEvenWhenCopyThrows() throws CopyException, IOException {
    Mockito.doThrow(new CopyException("error", new Exception())).when(copier)
        .copy(importAuthData, exportAuthData, jobId, Optional.of(exportInfo));
    processor.processJob();
    Mockito.verify(copier).getErrors(jobId);
  }
}
