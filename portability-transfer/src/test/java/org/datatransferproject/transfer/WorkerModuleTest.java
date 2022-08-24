package org.datatransferproject.transfer;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.common.collect.ImmutableList;
import org.datatransferproject.api.launcher.ExtensionContext;
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

  private static TransferExtension createTransferExtension(String serviceId) {
    return new TransferExtension() {
      @Override
      public String getServiceId() {
        return serviceId;
      }

      @Override
      public Exporter<?, ?> getExporter(String transferDataType) {
        return null;
      }

      @Override
      public Importer<?, ?> getImporter(String transferDataType) {
        return null;
      }

      @Override
      public void initialize(ExtensionContext context) {
      }
    };
  }
}
