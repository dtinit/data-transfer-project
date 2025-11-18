package org.datatransferproject.datatransfer.google.common;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;

import com.google.api.client.util.store.DataStore;
import com.google.common.base.Throwables;
import java.io.IOException;
import java.util.UUID;
import org.datatransferproject.types.transfer.errors.ErrorDetail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class GoogleErrorLoggerTest {
  private DataStore dataStore;
  private UUID jobId;

  @BeforeEach
  public void setup() {
    dataStore = mock(DataStore.class);
    jobId = UUID.randomUUID();
  }

  @Test
  public void test_createErrorDetail() {
    String id = "testId";
    String title = "testTitle";
    Exception exception =  new IOException();
    boolean canSkip = false;

    ErrorDetail expected = ErrorDetail.builder()
        .setId(id)
        .setTitle(title)
        .setException(Throwables.getStackTraceAsString(exception))
        .setCanSkip(canSkip)
        .build();

    ErrorDetail result = GoogleErrorLogger.createErrorDetail(
        id,
        title,
        exception,
        canSkip
    );

    assertThat(result).isEqualTo(expected);
  }
}
