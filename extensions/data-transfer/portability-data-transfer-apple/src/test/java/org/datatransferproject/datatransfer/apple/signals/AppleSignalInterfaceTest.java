package org.datatransferproject.datatransfer.apple.signals;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import java.util.UUID;

import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.spi.transfer.provider.SignalRequest;
import org.datatransferproject.spi.transfer.types.signals.JobLifeCycle;
import org.datatransferproject.spi.transfer.types.signals.JobLifeCycle.EndReason;
import org.datatransferproject.spi.transfer.types.signals.JobLifeCycle.State;
import org.datatransferproject.types.common.models.DataVertical;
import org.datatransferproject.types.transfer.auth.AppCredentials;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class AppleSignalInterfaceTest {
    private AppleSignalInterface signalInterface;

    @BeforeEach
    public void setUp() {
        TokensAndUrlAuthData authData = mock(TokensAndUrlAuthData.class);
        AppCredentials appCredentials = mock(AppCredentials.class);
        Monitor monitor = mock(Monitor.class);

        signalInterface = new AppleSignalInterface(authData, appCredentials, monitor);
    }

    @Test
    public void testSendSignal() {
        JobLifeCycle jobStatus = JobLifeCycle.builder()
            .setState(State.ENDED)
            .setEndReason(EndReason.SUCCESSFULLY_COMPLETED)
            .build();

        SignalRequest signalRequest =
            SignalRequest.builder()
                .setJobId(UUID.randomUUID().toString())
                .setJobStatus(jobStatus)
                .setExportingService("EXPORT_SERVICE")
                .setImportingService("IMPORT_SERVICE")
                .setDataType(DataVertical.MAIL.getDataType())
                .build();

        IllegalStateException thrown =
            assertThrows(
                IllegalStateException.class,
                () -> {
                    signalInterface.sendSignal(signalRequest);
                });
//         TODO: remove assertThrows() once we have a JobMetadata test-only-fixture that lets us call JobMetadata.init(...) in unit tests.

        assertThat(thrown.getMessage()).contains("JobMetadata must be initialized");
    }
}
