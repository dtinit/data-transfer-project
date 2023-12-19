package org.datatransferproject.spi.transfer.idempotentexecutor;

import com.google.common.collect.ImmutableList;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.types.transfer.retry.*;
import org.junit.Test;

import java.io.IOException;
import java.util.Optional;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

public class RetryingInMemoryIdempotentImporterExecutorTest {

    @Test
    public void skipRetryStrategy_executeAndSwallowIOExceptions() throws Exception {
        // Retries and then by default ALWAYS skips
        RetryStrategy skipRetryStrategy = new SkipRetryStrategy();
        RetryingInMemoryIdempotentImportExecutor retryingExecutor = new RetryingInMemoryIdempotentImportExecutor(
                mock(Monitor.class),
                new RetryStrategyLibrary(
                        ImmutableList.of(),
                        skipRetryStrategy
                )
        );

        assertThat(
               Optional.ofNullable(
                       retryingExecutor.executeAndSwallowIOExceptions(
                               "id",
                               "name",
                               () -> { throw new IOException("Test IO exception");}
                               )
               )
       ).isEqualTo(Optional.empty());

        assertThat(
                Optional.ofNullable(
                        retryingExecutor.executeAndSwallowIOExceptions(
                                "id",
                                "name",
                                () -> { throw new Exception("Test generic exception");}
                        )
                )
        ).isEqualTo(Optional.empty());

        assertThat(
                Optional.ofNullable(
                        retryingExecutor.executeAndSwallowIOExceptions(
                                "id",
                                "name",
                                () -> { throw new NullPointerException("Test null pointer exception");}
                        )
                )
        ).isEqualTo(Optional.empty());
    }

    @Test
    public void skipRetryStrategy_executeOrThrowException() throws Exception {
        // Retries and then by default ALWAYS skips
        RetryStrategy skipRetryStrategy = new SkipRetryStrategy();
        RetryingInMemoryIdempotentImportExecutor retryingExecutor = new RetryingInMemoryIdempotentImportExecutor(
                mock(Monitor.class),
                new RetryStrategyLibrary(
                        ImmutableList.of(),
                        skipRetryStrategy
                )
        );

        // Execute or throw does NOT throw, as the RetryStrategy has default of skip=true.
        assertThat(
                Optional.ofNullable(
                        retryingExecutor.executeOrThrowException(
                                "id",
                                "name",
                                () -> { throw new IOException("Test IO exception");}
                        )
                )
        ).isEqualTo(Optional.empty());

        assertThat(
                Optional.ofNullable(
                        retryingExecutor.executeOrThrowException(
                                "id",
                                "name",
                                () -> { throw new Exception("Test generic exception");}
                        )
                )
        ).isEqualTo(Optional.empty());

        assertThat(
                Optional.ofNullable(
                        retryingExecutor.executeOrThrowException(
                                "id",
                                "name",
                                () -> { throw new NullPointerException("Test null pointer exception");}
                        )
                )
        ).isEqualTo(Optional.empty());
    }

    @Test
    public void uniformRetryStrategy_executeAndSwallowIOExceptions() throws Exception {
        // Retries and then by default does NOT skip
        RetryStrategy uniformRetryStrategy = new UniformRetryStrategy(
                /* maxAttempts = */ 3,
                /* intervalMillis = */ 100,
                /* identifier = */ "identifier"
        );
        RetryingInMemoryIdempotentImportExecutor retryingExecutor = new RetryingInMemoryIdempotentImportExecutor(
                mock(Monitor.class),
                new RetryStrategyLibrary(
                        ImmutableList.of(),
                        uniformRetryStrategy
                )
        );

        // IO exceptions swallowed
        assertThat(
                Optional.ofNullable(
                        retryingExecutor.executeAndSwallowIOExceptions(
                                "id",
                                "name",
                                () -> { throw new IOException("Test IO exception");}
                        )
                )
        ).isEqualTo(Optional.empty());

        // Non-IO thrown
        assertThrows(
                NullPointerException.class,
                () -> retryingExecutor.executeAndSwallowIOExceptions(
                        "id",
                        "name",
                        () -> { throw new NullPointerException("Test NullPointer exception");}
                )
        );
    }

    @Test
    public void uniformRetryStrategy_executeOrThrowException() throws Exception {
        // Retries and then by default does NOT skip
        RetryStrategy uniformRetryStrategy = new UniformRetryStrategy(
                /* maxAttempts = */ 3,
                /* intervalMillis = */ 1,
                /* identifier = */ "identifier"
        );
        RetryingInMemoryIdempotentImportExecutor retryingExecutor = new RetryingInMemoryIdempotentImportExecutor(
                mock(Monitor.class),
                new RetryStrategyLibrary(
                        ImmutableList.of(),
                        uniformRetryStrategy
                )
        );

        // IO exceptions thrown
        assertThrows(
                IOException.class,
                () -> retryingExecutor.executeOrThrowException(
                        "id",
                        "name",
                        () -> { throw new IOException("Test IO exception");}
                )
        );

        // Non-IO thrown as well
        assertThrows(
                NullPointerException.class,
                () -> retryingExecutor.executeOrThrowException(
                        "id",
                        "name",
                        () -> { throw new NullPointerException("Test NullPointer exception");}
                )
        );
    }

    @Test
    public void noRetryStrategy_executeAndSwallowIOExceptions() throws Exception {
        // Retries and then by default does NOT skip
        RetryStrategy noRetryStrategy = new NoRetryStrategy();
        RetryingInMemoryIdempotentImportExecutor retryingExecutor = new RetryingInMemoryIdempotentImportExecutor(
                mock(Monitor.class),
                new RetryStrategyLibrary(
                        ImmutableList.of(),
                        noRetryStrategy
                )
        );

        // IO exceptions swallowed
        assertThat(
                Optional.ofNullable(
                        retryingExecutor.executeAndSwallowIOExceptions(
                                "id",
                                "name",
                                () -> { throw new IOException("Test IO exception");}
                        )
                )
        ).isEqualTo(Optional.empty());

        // Non-IO thrown
        assertThrows(
                NullPointerException.class,
                () -> retryingExecutor.executeAndSwallowIOExceptions(
                        "id",
                        "name",
                        () -> { throw new NullPointerException("Test NullPointer exception");}
                )
        );
    }

    @Test
    public void noRetryStrategy_executeOrThrowException() {
        // Retries and then by default does NOT skip
        RetryStrategy noRetryStrategy = new NoRetryStrategy();
        RetryingInMemoryIdempotentImportExecutor retryingExecutor = new RetryingInMemoryIdempotentImportExecutor(
                mock(Monitor.class),
                new RetryStrategyLibrary(
                        ImmutableList.of(),
                        noRetryStrategy
                )
        );

        // IO exceptions thrown
        assertThrows(
                IOException.class,
                () -> retryingExecutor.executeOrThrowException(
                        "id",
                        "name",
                        () -> { throw new IOException("Test IO exception");}
                )
        );

        // Non-IO thrown as well
        assertThrows(
                NullPointerException.class,
                () -> retryingExecutor.executeOrThrowException(
                        "id",
                        "name",
                        () -> { throw new NullPointerException("Test NullPointer exception");}
                )
        );
    }


    @Test
    public void exponentialBackoffStrategy_executeAndSwallowIOExceptions() throws Exception {
        // Retries and then by default does NOT skip
        RetryStrategy exponentialBackoffStrategy = new ExponentialBackoffStrategy(
                /* maxAttempts = */ 3,
                /* intervalMillis = */ 1,
                /* multiplier = */ 1,
                /* identifier = */ "identifier"
        );
        RetryingInMemoryIdempotentImportExecutor retryingExecutor = new RetryingInMemoryIdempotentImportExecutor(
                mock(Monitor.class),
                new RetryStrategyLibrary(
                        ImmutableList.of(),
                        exponentialBackoffStrategy
                )
        );

        // IO exceptions swallowed
        assertThat(
                Optional.ofNullable(
                        retryingExecutor.executeAndSwallowIOExceptions(
                                "id",
                                "name",
                                () -> { throw new IOException("Test IO exception");}
                        )
                )
        ).isEqualTo(Optional.empty());

        // Non-IO thrown
        assertThrows(
                NullPointerException.class,
                () -> retryingExecutor.executeAndSwallowIOExceptions(
                        "id",
                        "name",
                        () -> { throw new NullPointerException("Test NullPointer exception");}
                )
        );
    }

    @Test
    public void exponentialBackoffStrategy_executeOrThrowException() {
        // Retries and then by default does NOT skip
        RetryStrategy exponentialBackoffStrategy = new ExponentialBackoffStrategy(
                /* maxAttempts = */ 3,
                /* intervalMillis = */ 1,
                /* multiplier = */ 1,
                /* identifier = */ "identifier"
        );
        RetryingInMemoryIdempotentImportExecutor retryingExecutor = new RetryingInMemoryIdempotentImportExecutor(
                mock(Monitor.class),
                new RetryStrategyLibrary(
                        ImmutableList.of(),
                        exponentialBackoffStrategy
                )
        );

        // IO exceptions thrown
        assertThrows(
                IOException.class,
                () -> retryingExecutor.executeOrThrowException(
                        "id",
                        "name",
                        () -> {
                            throw new IOException("Test IO exception");
                        }
                )
        );

        // Non-IO thrown as well
        assertThrows(
                NullPointerException.class,
                () -> retryingExecutor.executeOrThrowException(
                        "id",
                        "name",
                        () -> {
                            throw new NullPointerException("Test NullPointer exception");
                        }
                )
        );
    }
}
