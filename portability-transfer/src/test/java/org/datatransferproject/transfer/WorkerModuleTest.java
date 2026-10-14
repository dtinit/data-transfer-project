package org.datatransferproject.transfer;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.inject.Guice;
import com.google.inject.Injector;
import java.util.OptionalLong;
import org.datatransferproject.api.launcher.Constants.Environment;
import org.datatransferproject.api.launcher.DtpInternalMetricRecorder;
import org.datatransferproject.api.launcher.ExtensionContext;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.api.launcher.TypeManager;
import org.datatransferproject.security.SymmetricKeyGenerator;
import org.datatransferproject.spi.cloud.extension.CloudExtension;
import org.datatransferproject.spi.cloud.storage.JobStore;
import org.datatransferproject.spi.transfer.hooks.JobHooks;
import org.datatransferproject.spi.transfer.idempotentexecutor.IdempotentImportExecutor;
import org.datatransferproject.spi.transfer.idempotentexecutor.IdempotentImportExecutorExtension;
import org.datatransferproject.spi.transfer.provider.TransferCompatibilityProvider;
import org.datatransferproject.spi.transfer.security.AuthDataDecryptService;
import org.datatransferproject.spi.transfer.security.SecurityExtension;
import org.datatransferproject.types.common.models.DataVertical;
import org.datatransferproject.spi.transfer.extension.TransferExtension;
import org.datatransferproject.spi.transfer.provider.Exporter;
import org.datatransferproject.spi.transfer.provider.Importer;
import org.junit.jupiter.api.Test;
public class WorkerModuleTest {

  private static TransferExtension FOO_UPPER = createTransferExtension("FOO");
  private static TransferExtension FOO_LOWER = createTransferExtension("foo");
  private static TransferExtension BAR_UPPER = createTransferExtension("BAR");
  private static TransferExtension BAR_LOWER = createTransferExtension("bar");

  @Test
  public void findTransferExtension() {
    ImmutableList<TransferExtension> transferExtensions = ImmutableList.of(FOO_UPPER, BAR_UPPER);
    assertThat(WorkerModule.findTransferExtension(transferExtensions, "FOO")).isEqualTo(FOO_UPPER);
    assertThat(WorkerModule.findTransferExtension(transferExtensions, "foo")).isEqualTo(FOO_UPPER);
  }

  @Test
  public void findTransferExtension_mixedCasing() {
    ImmutableList<TransferExtension> transferExtensions = ImmutableList.of(FOO_UPPER, BAR_LOWER);
    assertThat(WorkerModule.findTransferExtension(transferExtensions, "FOO")).isEqualTo(FOO_UPPER);
    assertThat(WorkerModule.findTransferExtension(transferExtensions, "foo")).isEqualTo(FOO_UPPER);
    assertThat(WorkerModule.findTransferExtension(transferExtensions, "BAR")).isEqualTo(BAR_LOWER);
    assertThat(WorkerModule.findTransferExtension(transferExtensions, "bar")).isEqualTo(BAR_LOWER);
  }

  @Test
  public void findTransferExtension_noMatch() {
    ImmutableList<TransferExtension> transferExtensions = ImmutableList.of(FOO_UPPER, BAR_UPPER);
    assertThrows(IllegalStateException.class,
        () -> WorkerModule.findTransferExtension(transferExtensions, "BAZ"));
  }

  @Test
  public void findTransferExtension_duplicateMatches() {
    ImmutableList<TransferExtension> transferExtensions = ImmutableList.of(FOO_UPPER, FOO_LOWER);
    assertThrows(IllegalStateException.class,
        () -> WorkerModule.findTransferExtension(transferExtensions, "FOO"));
  }

  @Test
  public void toOptionalLong_readsNumbersAndNumericStrings() {
    assertThat(WorkerModule.toOptionalLong(300000)).isEqualTo(OptionalLong.of(300000));
    assertThat(WorkerModule.toOptionalLong(604800L)).isEqualTo(OptionalLong.of(604800));
    assertThat(WorkerModule.toOptionalLong(" 60 ")).isEqualTo(OptionalLong.of(60));
  }

  @Test
  public void toOptionalLong_isEmptyForMissingOrNonNumericValues() {
    assertThat(WorkerModule.toOptionalLong(null)).isEqualTo(OptionalLong.empty());
    assertThat(WorkerModule.toOptionalLong("a week")).isEqualTo(OptionalLong.empty());
    assertThat(WorkerModule.toOptionalLong(true)).isEqualTo(OptionalLong.empty());
  }

  @Test
  public void onUnlessTurnedOff_isOnWhenNotSetOrTrue() {
    assertThat(WorkerModule.onUnlessTurnedOff(null)).isTrue();
    assertThat(WorkerModule.onUnlessTurnedOff(true)).isTrue();
    assertThat(WorkerModule.onUnlessTurnedOff(" TRUE ")).isTrue();
  }

  @Test
  public void onUnlessTurnedOff_isOffForFalseOrAnythingElse() {
    assertThat(WorkerModule.onUnlessTurnedOff(false)).isFalse();
    assertThat(WorkerModule.onUnlessTurnedOff("false")).isFalse();
    // A mistyped value turns it off instead of failing the worker at startup.
    assertThat(WorkerModule.onUnlessTurnedOff("no")).isFalse();
    assertThat(WorkerModule.onUnlessTurnedOff(0)).isFalse();
  }

  @Test
  public void telemetrySettings_defaults() {
    WorkerModule module = workerModuleWithNoSettings();

    // On by default, so handlers that opt in get telemetry without any setup by the host.
    assertThat(module.transferTelemetryEnabled()).isTrue();
    assertThat(module.telemetryHeartbeatInterval()).isEqualTo(OptionalLong.empty());
    assertThat(module.maxWorkerLifetimeSeconds()).isEqualTo(OptionalLong.empty());
  }

  @Test
  public void transferSignals_isOneInstanceForTheJobAndTheCancelWatcher() {
    Injector injector = Guice.createInjector(workerModuleWithNoSettings());

    TransferSignals signals = injector.getInstance(TransferSignals.class);

    // The cancel watcher must see the telemetry state of the job that JobProcessor runs.
    assertThat(injector.getInstance(TransferSignals.class)).isSameAs(signals);
    assertThat(injector.getInstance(JobProcessor.class)).isNotNull();
    assertThat(injector.getInstance(JobCancelWatchingService.class)).isNotNull();
  }

  /** A WorkerModule with no settings, so every setting reads as its default. */
  private static WorkerModule workerModuleWithNoSettings() {
    ExtensionContext context = mock(ExtensionContext.class);
    when(context.cloud()).thenReturn("local");
    when(context.environment()).thenReturn(Environment.LOCAL);
    when(context.getMonitor()).thenReturn(mock(Monitor.class));
    TypeManager typeManager = mock(TypeManager.class);
    when(typeManager.getMapper()).thenReturn(new ObjectMapper());
    when(context.getTypeManager()).thenReturn(typeManager);
    when(context.getService(DtpInternalMetricRecorder.class))
        .thenReturn(mock(DtpInternalMetricRecorder.class));
    when(context.getSetting(anyString(), any())).thenAnswer(call -> call.getArgument(1));

    CloudExtension cloudExtension = mock(CloudExtension.class);
    when(cloudExtension.getJobStore()).thenReturn(mock(JobStore.class));
    SecurityExtension securityExtension = mock(SecurityExtension.class);
    when(securityExtension.getDecryptService()).thenReturn(mock(AuthDataDecryptService.class));
    IdempotentImportExecutorExtension executors = mock(IdempotentImportExecutorExtension.class);
    when(executors.getIdempotentImportExecutor(context))
        .thenReturn(mock(IdempotentImportExecutor.class));
    when(executors.getRetryingIdempotentImportExecutor(context))
        .thenReturn(mock(IdempotentImportExecutor.class));

    return new WorkerModule(
        context,
        cloudExtension,
        ImmutableList.of(),
        securityExtension,
        executors,
        mock(SymmetricKeyGenerator.class),
        mock(JobHooks.class),
        new TransferCompatibilityProvider());
  }

  private static TransferExtension createTransferExtension(String serviceId) {
    return new TransferExtension() {
      @Override
      public String getServiceId() {
        return serviceId;
      }

      @Override
      public Exporter<?, ?> getExporter(DataVertical transferDataType) {
        return null;
      }

      @Override
      public Importer<?, ?> getImporter(DataVertical transferDataType) {
        return null;
      }

      @Override
      public void initialize(ExtensionContext context) {
      }
    };
  }
}
