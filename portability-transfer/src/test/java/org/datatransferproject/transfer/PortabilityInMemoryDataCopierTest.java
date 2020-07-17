/*
 * Copyright 2020 The Data Transfer Project Authors.
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

import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.spi.transfer.provider.ExportResult;
import org.datatransferproject.spi.transfer.types.ContinuationData;
import org.datatransferproject.spi.transfer.types.CopyException;
import org.datatransferproject.test.types.FakeIdempotentImportExecutor;
import org.datatransferproject.types.common.ExportInformation;
import org.datatransferproject.types.common.PaginationData;
import org.datatransferproject.types.common.models.ContainerResource;
import org.datatransferproject.types.transfer.auth.AuthData;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.InOrder;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

@RunWith(Parameterized.class)
public class PortabilityInMemoryDataCopierTest {
  private UUID jobId;
  private String jobIdPrefix;
  private ExportInformation exportInfo;
  private ExportResult<?> initialExportResult;
  private ContinuationData continuationData;
  private AuthData exportAuthData;
  private AuthData importAuthData;
  private PortabilityAbstractInMemoryDataCopier inMemoryDataCopier;

  private static class TestPortabilityInMemoryDataCopier extends PortabilityInMemoryDataCopier {
    public TestPortabilityInMemoryDataCopier() {
      super(
          null,
          null,
          null,
          Mockito.mock(Monitor.class),
          new FakeIdempotentImportExecutor(),
          null,
          null);
    }

    protected ExportResult<?> copyIteration(
        UUID jobId,
        AuthData exportAuthData,
        AuthData importAuthData,
        Optional<ExportInformation> exportInformation,
        String jobIdPrefix,
        int copyIteration)
        throws CopyException {
      return Mockito.mock(ExportResult.class);
    }
  }

  private static class TestPortabilityStackInMemoryDataCopier
      extends PortabilityStackInMemoryDataCopier {
    public TestPortabilityStackInMemoryDataCopier() {
      super(
          null,
          null,
          null,
          Mockito.mock(Monitor.class),
          new FakeIdempotentImportExecutor(),
          null,
          null);
    }

    protected ExportResult<?> copyIteration(
        UUID jobId,
        AuthData exportAuthData,
        AuthData importAuthData,
        Optional<ExportInformation> exportInformation,
        String jobIdPrefix,
        int copyIteration)
        throws CopyException {
      return Mockito.mock(ExportResult.class);
    }
  }

  @Parameterized.Parameters
  public static Iterable<PortabilityAbstractInMemoryDataCopier> data() {
    return Arrays.asList(
        (new PortabilityAbstractInMemoryDataCopier[] {
          Mockito.spy(new TestPortabilityInMemoryDataCopier()),
          Mockito.spy(new TestPortabilityStackInMemoryDataCopier())
        }));
  }

  public PortabilityInMemoryDataCopierTest(PortabilityAbstractInMemoryDataCopier copier) {
    inMemoryDataCopier = copier;
  }

  @Before
  public void setUp() throws CopyException, IOException {
    importAuthData = exportAuthData = Mockito.mock(AuthData.class);
    jobId = UUID.randomUUID();
    jobIdPrefix = "Job " + jobId + ": ";
    exportInfo = Mockito.mock(ExportInformation.class);
    initialExportResult = Mockito.mock(ExportResult.class);
    continuationData = Mockito.mock(ContinuationData.class);
    // reset counters
    TestPortabilityInMemoryDataCopier.COPY_ITERATION_COUNTER.set(0);
    TestPortabilityStackInMemoryDataCopier.COPY_ITERATION_COUNTER.set(0);
  }

  @Test
  public void initialExportInfoIsNull() throws CopyException, IOException {

    // Arrange
    Optional<ExportInformation> initialExportInfo = Optional.empty();

    // Act
    inMemoryDataCopier.copy(exportAuthData, importAuthData, jobId, initialExportInfo);

    // Assert
    Mockito.verify(inMemoryDataCopier)
        .copyIteration(jobId, exportAuthData, importAuthData, initialExportInfo, jobIdPrefix, 1);
  }

  @Test
  public void continuationDataIsNull() throws CopyException, IOException {

    // Arrange
    Mockito.when(initialExportResult.getContinuationData()).thenReturn(null);
    Mockito.doReturn(initialExportResult)
        .when(inMemoryDataCopier)
        .copyIteration(
            jobId, exportAuthData, importAuthData, Optional.of(exportInfo), jobIdPrefix, 1);
    // Act
    inMemoryDataCopier.copy(exportAuthData, importAuthData, jobId, Optional.of(exportInfo));

    // Assert copyIteration only called once
    Mockito.verify(inMemoryDataCopier)
        .copyIteration(
            jobId, exportAuthData, importAuthData, Optional.of(exportInfo), jobIdPrefix, 1);
  }

  @Test
  public void continuationDataWithPaginationData() throws CopyException, IOException {

    // Arrange
    PaginationData paginationData = Mockito.mock(PaginationData.class);
    ExportInformation paginationExportInfo = new ExportInformation(paginationData, null);

    Mockito.when(continuationData.getPaginationData()).thenReturn(paginationData);
    Mockito.when(initialExportResult.getContinuationData()).thenReturn(continuationData);
    Mockito.doReturn(initialExportResult)
        .when(inMemoryDataCopier)
        .copyIteration(
            jobId, exportAuthData, importAuthData, Optional.of(exportInfo), jobIdPrefix, 1);

    // Act
    inMemoryDataCopier.copy(exportAuthData, importAuthData, jobId, Optional.of(exportInfo));

    // Assert
    InOrder orderVerifier = Mockito.inOrder(inMemoryDataCopier);
    orderVerifier.verify(inMemoryDataCopier)
        .copyIteration(
            jobId, exportAuthData, importAuthData, Optional.of(exportInfo), jobIdPrefix, 1);
    orderVerifier.verify(inMemoryDataCopier)
        .copyIteration(
            jobId,
            exportAuthData,
            importAuthData,
            Optional.of(paginationExportInfo),
            jobIdPrefix,
            2);
  }

  @Test
  public void continuationDataWithEmptySubResourceList() throws CopyException, IOException {

    // Arrange
    Mockito.when(continuationData.getContainerResources()).thenReturn(new ArrayList<>());
    Mockito.when(initialExportResult.getContinuationData()).thenReturn(continuationData);
    Mockito.doReturn(initialExportResult)
        .when(inMemoryDataCopier)
        .copyIteration(
            jobId, exportAuthData, importAuthData, Optional.of(exportInfo), jobIdPrefix, 1);

    // Act
    inMemoryDataCopier.copy(exportAuthData, importAuthData, jobId, Optional.of(exportInfo));

    // Assert
    Mockito.verify(inMemoryDataCopier)
        .copyIteration(
            jobId, exportAuthData, importAuthData, Optional.of(exportInfo), jobIdPrefix, 1);
  }

  @Test
  public void continuationDataWithSingleSubResource() throws CopyException, IOException {

    // Arrange
    ContainerResource subResource = Mockito.mock(ContainerResource.class);

    ExportInformation subResourceExportInfo = new ExportInformation(null, subResource);

    Mockito.when(continuationData.getContainerResources()).thenReturn(Arrays.asList(subResource));
    Mockito.when(initialExportResult.getContinuationData()).thenReturn(continuationData);
    Mockito.doReturn(initialExportResult)
        .when(inMemoryDataCopier)
        .copyIteration(
            jobId, exportAuthData, importAuthData, Optional.of(exportInfo), jobIdPrefix, 1);

    // Act
    inMemoryDataCopier.copy(exportAuthData, importAuthData, jobId, Optional.of(exportInfo));

    // Assert
    InOrder orderVerifier = Mockito.inOrder(inMemoryDataCopier);
    orderVerifier.verify(inMemoryDataCopier)
        .copyIteration(
            jobId, exportAuthData, importAuthData, Optional.of(exportInfo), jobIdPrefix, 1);
    orderVerifier.verify(inMemoryDataCopier)
        .copyIteration(
            jobId,
            exportAuthData,
            importAuthData,
            Optional.of(subResourceExportInfo),
            jobIdPrefix,
            2);
  }

  @Test
  public void continuationDataWithMultipleSubResources() throws CopyException, IOException {

    // Arrange
    ContainerResource subResource1 = Mockito.mock(ContainerResource.class);
    ContainerResource subResource2 = Mockito.mock(ContainerResource.class);

    ExportInformation subResource1ExportInfo = new ExportInformation(null, subResource1);
    ExportInformation subResource2ExportInfo = new ExportInformation(null, subResource2);

    Mockito.when(continuationData.getContainerResources())
        .thenReturn(Arrays.asList(subResource1, subResource2));
    Mockito.when(initialExportResult.getContinuationData()).thenReturn(continuationData);
    Mockito.doReturn(initialExportResult)
        .when(inMemoryDataCopier)
        .copyIteration(
            jobId, exportAuthData, importAuthData, Optional.of(exportInfo), jobIdPrefix, 1);

    // Act
    inMemoryDataCopier.copy(exportAuthData, importAuthData, jobId, Optional.of(exportInfo));

    // Assert
    InOrder orderVerifier = Mockito.inOrder(inMemoryDataCopier);
    orderVerifier.verify(inMemoryDataCopier)
        .copyIteration(
            jobId, exportAuthData, importAuthData, Optional.of(exportInfo), jobIdPrefix, 1);
    orderVerifier.verify(inMemoryDataCopier)
        .copyIteration(
            jobId,
            exportAuthData,
            importAuthData,
            Optional.of(subResource1ExportInfo),
            jobIdPrefix,
            2);
    orderVerifier.verify(inMemoryDataCopier)
        .copyIteration(
            jobId,
            exportAuthData,
            importAuthData,
            Optional.of(subResource2ExportInfo),
            jobIdPrefix,
            3);
  }

  @Test
  public void continuationDataWithPaginationDataAndMultipleSubResources()
      throws CopyException, IOException {

    // Arrange
    PaginationData paginationData = Mockito.mock(PaginationData.class);
    ContainerResource subResource1 = Mockito.mock(ContainerResource.class);
    ContainerResource subResource2 = Mockito.mock(ContainerResource.class);

    ExportInformation paginationExportInfo = new ExportInformation(paginationData, null);
    ExportInformation subResource1ExportInfo = new ExportInformation(null, subResource1);
    ExportInformation subResource2ExportInfo = new ExportInformation(null, subResource2);

    Mockito.when(continuationData.getPaginationData()).thenReturn(paginationData);
    Mockito.when(continuationData.getContainerResources())
        .thenReturn(Arrays.asList(subResource1, subResource2));
    Mockito.when(initialExportResult.getContinuationData()).thenReturn(continuationData);
    Mockito.doReturn(initialExportResult)
        .when(inMemoryDataCopier)
        .copyIteration(
            jobId, exportAuthData, importAuthData, Optional.of(exportInfo), jobIdPrefix, 1);

    // Act
    inMemoryDataCopier.copy(exportAuthData, importAuthData, jobId, Optional.of(exportInfo));

    // Assert
    InOrder orderVerifier = Mockito.inOrder(inMemoryDataCopier);
    orderVerifier.verify(inMemoryDataCopier)
        .copyIteration(
            jobId, exportAuthData, importAuthData, Optional.of(exportInfo), jobIdPrefix, 1);
    orderVerifier.verify(inMemoryDataCopier)
        .copyIteration(
            jobId,
            exportAuthData,
            importAuthData,
            Optional.of(paginationExportInfo),
            jobIdPrefix,
            2);
    orderVerifier.verify(inMemoryDataCopier)
        .copyIteration(
            jobId,
            exportAuthData,
            importAuthData,
            Optional.of(subResource1ExportInfo),
            jobIdPrefix,
            3);
    orderVerifier.verify(inMemoryDataCopier)
        .copyIteration(
            jobId,
            exportAuthData,
            importAuthData,
            Optional.of(subResource2ExportInfo),
            jobIdPrefix,
            4);
  }

  @Test
  public void continuationDataWithPaginationDataAndNestedSubResource()
      throws CopyException, IOException {

    // Arrange
    PaginationData paginationData = Mockito.mock(PaginationData.class);
    ContinuationData paginationContinuationData = Mockito.mock(ContinuationData.class);
    ExportResult<?> paginationExportResult = Mockito.mock(ExportResult.class);

    ContainerResource subResource = Mockito.mock(ContainerResource.class);
    ContainerResource paginationSubResource = Mockito.mock(ContainerResource.class);

    ExportInformation paginationExportInfo = new ExportInformation(paginationData, null);
    ExportInformation subResourceExportInfo = new ExportInformation(null, subResource);
    ExportInformation paginationSubResourceExportInfo =
        new ExportInformation(null, paginationSubResource);

    Mockito.when(continuationData.getPaginationData()).thenReturn(paginationData);
    Mockito.when(continuationData.getContainerResources()).thenReturn(Arrays.asList(subResource));
    Mockito.when(paginationContinuationData.getContainerResources())
        .thenReturn(Arrays.asList(paginationSubResource));

    Mockito.when(initialExportResult.getContinuationData()).thenReturn(continuationData);
    Mockito.when(paginationExportResult.getContinuationData())
        .thenReturn(paginationContinuationData);

    Mockito.doReturn(initialExportResult)
        .when(inMemoryDataCopier)
        .copyIteration(
            jobId, exportAuthData, importAuthData, Optional.of(exportInfo), jobIdPrefix, 1);
    Mockito.doReturn(paginationExportResult)
        .when(inMemoryDataCopier)
        .copyIteration(
            jobId,
            exportAuthData,
            importAuthData,
            Optional.of(paginationExportInfo),
            jobIdPrefix,
            2);

    // Act
    inMemoryDataCopier.copy(exportAuthData, importAuthData, jobId, Optional.of(exportInfo));

    // Assert
    InOrder orderVerifier = Mockito.inOrder(inMemoryDataCopier);
    orderVerifier.verify(inMemoryDataCopier)
        .copyIteration(
            jobId, exportAuthData, importAuthData, Optional.of(exportInfo), jobIdPrefix, 1);
    orderVerifier.verify(inMemoryDataCopier)
        .copyIteration(
            jobId,
            exportAuthData,
            importAuthData,
            Optional.of(paginationExportInfo),
            jobIdPrefix,
            2);
    orderVerifier.verify(inMemoryDataCopier)
        .copyIteration(
            jobId,
            exportAuthData,
            importAuthData,
            Optional.of(paginationSubResourceExportInfo),
            jobIdPrefix,
            3);
    orderVerifier.verify(inMemoryDataCopier)
        .copyIteration(
            jobId,
            exportAuthData,
            importAuthData,
            Optional.of(subResourceExportInfo),
            jobIdPrefix,
            4);
  }
}

