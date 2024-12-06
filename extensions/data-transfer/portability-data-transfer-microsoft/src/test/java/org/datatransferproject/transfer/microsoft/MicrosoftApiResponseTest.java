package org.datatransferproject.transfer.microsoft;

import static com.google.common.truth.Truth.assertThat;
import static org.datatransferproject.transfer.microsoft.MicrosoftApiResponse.CAUSE_PREFIX_UNRECOGNIZED_EXCEPTION;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import okhttp3.MediaType;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.datatransferproject.spi.transfer.types.DestinationMemoryFullException;
import org.datatransferproject.spi.transfer.types.PermissionDeniedException;
import org.junit.Test;

public class MicrosoftApiResponseTest {
  @Test
  public void testOkay() throws IOException {
    Response networkResponse = mock(Response.class);
    when(networkResponse.code()).thenReturn(200);
    when(networkResponse.message()).thenReturn("hello world");
    when(networkResponse.body())
        .thenReturn(
            ResponseBody.create(
                MediaType.parse("application/json"),
                "{\"message\": \"Hippo is the best dog, not a real-world response\"}"));

    MicrosoftApiResponse response = MicrosoftApiResponse.ofResponse(networkResponse);

    assertThat(response.isOkay()).isTrue();
  }

  @Test
  public void testErrorPermission() throws IOException {
    Response networkResponse = mock(Response.class);
    when(networkResponse.code()).thenReturn(403);
    when(networkResponse.message()).thenReturn("Access Denied");

    MicrosoftApiResponse response = MicrosoftApiResponse.ofResponse(networkResponse);

    assertThat(response.isOkay()).isFalse();
    assertThrows(
        PermissionDeniedException.class,
        () -> {
          response.throwDtpException("unit testing");
        });
  }

  @Test
  public void testDestinationFull() throws IOException {
    Response networkResponse = mock(Response.class);
    when(networkResponse.code()).thenReturn(507);
    when(networkResponse.message()).thenReturn("");
    when(networkResponse.body())
        .thenReturn(
            ResponseBody.create(
                MediaType.parse("application/json"),
                "{\"message\": \"Insufficient Space Available\"}"));

    MicrosoftApiResponse response = MicrosoftApiResponse.ofResponse(networkResponse);

    assertThat(response.isOkay()).isFalse();
    assertThrows(
        DestinationMemoryFullException.class,
        () -> {
          response.throwDtpException("unit testing");
        });
  }

  @Test
  public void testErrorUnknown() throws IOException {
    Response networkResponse = mock(Response.class);
    when(networkResponse.code()).thenReturn(507);
    when(networkResponse.message()).thenReturn("");
    when(networkResponse.body())
        .thenReturn(
            ResponseBody.create(
                MediaType.parse("application/json"),
                "{\"message\": \"Hippo is the best dog, not a real-world response\"}"));

    MicrosoftApiResponse response = MicrosoftApiResponse.ofResponse(networkResponse);

    assertThat(response.isOkay()).isFalse();
    IOException thrown =
        assertThrows(
            IOException.class,
            () -> {
              response.throwDtpException("unit testing");
            });
    assertThat(thrown.getMessage()).contains(CAUSE_PREFIX_UNRECOGNIZED_EXCEPTION);
  }
}
