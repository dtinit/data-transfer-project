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
        RetryStrategy SKIP_RETRY_STRATEGY = new SkipRetryStrategy();
        RetryingInMemoryIdempotentImportExecutor retryingExecutor = new RetryingInMemoryIdempotentImportExecutor(
                mock(Monitor.class),
                new RetryStrategyLibrary( ImmutableList.of(), SKIP_RETRY_STRATEGY )
        );

        assertThat(
               Optional.ofNullable(
                       retryingExecutor.executeAndSwallowIOExceptions("id", "name",
                               () -> { throw new IOException("Test IO exception");}
                               )
               )
       ).isEqualTo(Optional.empty());

        assertThat(
                Optional.ofNullable(
                        retryingExecutor.executeAndSwallowIOExceptions("id", "name",
                                () -> { throw new Exception("Test generic exception");}
                        )
                )
        ).isEqualTo(Optional.empty());

        assertThat(
                Optional.ofNullable(
                        retryingExecutor.executeAndSwallowIOExceptions("id", "name",
                                () -> { throw new NullPointerException("Test null pointer exception");}
                        )
                )
        ).isEqualTo(Optional.empty());
    }

    @Test
    public void skipRetryStrategy_executeOrThrowException() throws Exception {
        // Retries and then by default ALWAYS skips
        RetryStrategy SKIP_RETRY_STRATEGY = new SkipRetryStrategy();
        RetryingInMemoryIdempotentImportExecutor retryingExecutor = new RetryingInMemoryIdempotentImportExecutor(
                mock(Monitor.class),
                new RetryStrategyLibrary( ImmutableList.of(), SKIP_RETRY_STRATEGY )
        );

        // Execute or throw does NOT throw, as the RetryStrategy has default of skip=true.
        assertThat(
                Optional.ofNullable(
                        retryingExecutor.executeOrThrowException("id", "name",
                                () -> { throw new IOException("Test IO exception");}
                        )
                )
        ).isEqualTo(Optional.empty());

        assertThat(
                Optional.ofNullable(
                        retryingExecutor.executeOrThrowException("id", "name",
                                () -> { throw new Exception("Test generic exception");}
                        )
                )
        ).isEqualTo(Optional.empty());

        assertThat(
                Optional.ofNullable(
                        retryingExecutor.executeOrThrowException("id", "name",
                                () -> { throw new NullPointerException("Test null pointer exception");}
                        )
                )
        ).isEqualTo(Optional.empty());
    }

    @Test
    public void uniformRetryStrategy_executeAndSwallowIOExceptions() throws Exception {
        // Retries and then by default does NOT skip
        RetryStrategy UNIFORM_RETRY_STRATEGY = new UniformRetryStrategy(
                /* maxAttempts = */ 3,
                /* intervalMillis = */ 100,
                /* identifier = */ "identifier"
        );
        RetryingInMemoryIdempotentImportExecutor retryingExecutor = new RetryingInMemoryIdempotentImportExecutor(
                mock(Monitor.class),
                new RetryStrategyLibrary( ImmutableList.of(), UNIFORM_RETRY_STRATEGY )
        );

        // IO exceptions swallowed
        assertThat(
                Optional.ofNullable(
                        retryingExecutor.executeAndSwallowIOExceptions("id", "name",
                                () -> { throw new IOException("Test IO exception");}
                        )
                )
        ).isEqualTo(Optional.empty());

        // Non-IO thrown
        assertThrows(
                NullPointerException.class,
                () -> retryingExecutor.executeAndSwallowIOExceptions("id", "name",
                        () -> { throw new NullPointerException("Test NullPointer exception");}
                )
        );
    }

    @Test
    public void uniformRetryStrategy_executeOrThrowException() throws Exception {
        // Retries and then by default does NOT skip
        RetryStrategy UNIFORM_RETRY_STRATEGY = new UniformRetryStrategy(
                /* maxAttempts = */ 3,
                /* intervalMillis = */ 1,
                /* identifier = */ "identifier"
        );
        RetryingInMemoryIdempotentImportExecutor retryingExecutor = new RetryingInMemoryIdempotentImportExecutor(
                mock(Monitor.class),
                new RetryStrategyLibrary( ImmutableList.of(), UNIFORM_RETRY_STRATEGY )
        );

        // IO exceptions thrown
        assertThrows(
                IOException.class,
                () -> retryingExecutor.executeOrThrowException("id", "name",
                        () -> { throw new IOException("Test IO exception");}
                )
        );

        // Non-IO thrown as well
        assertThrows(
                NullPointerException.class,
                () -> retryingExecutor.executeOrThrowException("id", "name",
                        () -> { throw new NullPointerException("Test NullPointer exception");}
                )
        );
    }

    @Test
    public void noRetryStrategy_executeAndSwallowIOExceptions() throws Exception {
        // Retries and then by default does NOT skip
        RetryStrategy NO_RETRY_STRATEGY = new NoRetryStrategy();
        RetryingInMemoryIdempotentImportExecutor retryingExecutor = new RetryingInMemoryIdempotentImportExecutor(
                mock(Monitor.class),
                new RetryStrategyLibrary(ImmutableList.of(), NO_RETRY_STRATEGY)
        );

        // IO exceptions swallowed
        assertThat(
                Optional.ofNullable(
                        retryingExecutor.executeAndSwallowIOExceptions("id", "name",
                                () -> { throw new IOException("Test IO exception");}
                        )
                )
        ).isEqualTo(Optional.empty());

        // Non-IO thrown
        assertThrows(
                NullPointerException.class,
                () -> retryingExecutor.executeAndSwallowIOExceptions("id", "name",
                        () -> { throw new NullPointerException("Test NullPointer exception");}
                )
        );
    }

    @Test
    public void noRetryStrategy_executeOrThrowException() {
        // Retries and then by default does NOT skip
        RetryStrategy NO_RETRY_STRATEGY = new NoRetryStrategy();
        RetryingInMemoryIdempotentImportExecutor retryingExecutor = new RetryingInMemoryIdempotentImportExecutor(
                mock(Monitor.class),
                new RetryStrategyLibrary( ImmutableList.of(), NO_RETRY_STRATEGY )
        );

        // IO exceptions thrown
        assertThrows(
                IOException.class,
                () -> retryingExecutor.executeOrThrowException("id", "name",
                        () -> { throw new IOException("Test IO exception");}
                )
        );

        // Non-IO thrown as well
        assertThrows(
                NullPointerException.class,
                () -> retryingExecutor.executeOrThrowException("id", "name",
                        () -> { throw new NullPointerException("Test NullPointer exception");}
                )
        );
    }


    @Test
    public void exponentialBackoffStrategy_executeAndSwallowIOExceptions() throws Exception {
        // Retries and then by default does NOT skip
        RetryStrategy EXPONENTIAL_BACKOFF_STRATEGY = new ExponentialBackoffStrategy(
                /* maxAttempts = */ 3,
                /* intervalMillis = */ 1,
                /* multiplier = */ 1,
                /* identifier = */ "identifier"
        );
        RetryingInMemoryIdempotentImportExecutor retryingExecutor = new RetryingInMemoryIdempotentImportExecutor(
                mock(Monitor.class),
                new RetryStrategyLibrary( ImmutableList.of(), EXPONENTIAL_BACKOFF_STRATEGY )
        );

        // IO exceptions swallowed
        assertThat(
                Optional.ofNullable(
                        retryingExecutor.executeAndSwallowIOExceptions("id", "name",
                                () -> { throw new IOException("Test IO exception");}
                        )
                )
        ).isEqualTo(Optional.empty());

        // Non-IO thrown
        assertThrows(
                NullPointerException.class,
                () -> retryingExecutor.executeAndSwallowIOExceptions("id", "name",
                        () -> { throw new NullPointerException("Test NullPointer exception");}
                )
        );
    }

    @Test
    public void exponentialBackoffStrategy_executeOrThrowException() {
        // Retries and then by default does NOT skip
        RetryStrategy EXPONENTIAL_BACKOFF_STRATEGY = new ExponentialBackoffStrategy(
                /* maxAttempts = */ 3,
                /* intervalMillis = */ 1,
                /* multiplier = */ 1,
                /* identifier = */ "identifier"
        );
        RetryingInMemoryIdempotentImportExecutor retryingExecutor = new RetryingInMemoryIdempotentImportExecutor(
                mock(Monitor.class),
                new RetryStrategyLibrary( ImmutableList.of(), EXPONENTIAL_BACKOFF_STRATEGY )
        );

        // IO exceptions thrown
        assertThrows(
                IOException.class,
                () -> retryingExecutor.executeOrThrowException("id", "name",
                        () -> {
                            throw new IOException("Test IO exception");
                        }
                )
        );

        // Non-IO thrown as well
        assertThrows(
                NullPointerException.class,
                () -> retryingExecutor.executeOrThrowException("id", "name",
                        () -> {
                            throw new NullPointerException("Test NullPointer exception");
                        }
                )
        );
    }
}
