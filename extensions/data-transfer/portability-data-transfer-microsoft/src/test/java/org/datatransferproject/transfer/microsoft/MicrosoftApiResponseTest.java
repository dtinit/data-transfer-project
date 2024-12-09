package org.datatransferproject.transfer.microsoft;

import static com.google.common.truth.Truth.assertThat;
import static okhttp3.Protocol.HTTP_2;
import static org.datatransferproject.transfer.microsoft.MicrosoftApiResponse.CAUSE_PREFIX_UNRECOGNIZED_EXCEPTION;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.datatransferproject.spi.transfer.types.DestinationMemoryFullException;
import org.datatransferproject.spi.transfer.types.PermissionDeniedException;
import org.junit.Test;

public class MicrosoftApiResponseTest {
  private static final ObjectMapper objectMapper =
      new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  @Test
  public void testOkay() throws IOException {
    Response networkResponse =
        fakeResponse(
                200,
                "hello world",
                "{\"message\": \"Hippo is the best dog, not a real-world response\"}")
            .build();

    MicrosoftApiResponse response = MicrosoftApiResponse.ofResponse(networkResponse);

    assertThat(response.isOkay()).isTrue();
  }

  @Test
  public void testErrorPermission() throws IOException {
    Response networkResponse = fakeResponse(403, "Access Denied").build();

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
    Response networkResponse =
        fakeResponse(507, "", "{\"message\": \"Insufficient Space Available\"}").build();

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
    Response networkResponse =
        fakeResponse(507, "", "{\"message\": \"Hippo is the best dog, not a real-world response\"}")
            .build();

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

  @Test // regression coverage for originating OKHTTP response being closed after constructing our
  // class
  public void testBytesOnClosedResponse() throws Exception {
    // It appears OKHTTP is sometimes constructing a response with a strange "body" state for which
    // bytes() throws an illegal state exception. Reproducing the exact exception hasn't been done
    // just yet, but in the meantime we at least run the same triggering code path
    // (MicrosoftApiResponse#getJsonValue()) to detect exceptions on strange bodies.
    Response networkResponse = fakeResponse(200, "OK", "" /*invalid json body*/).build();
    MicrosoftApiResponse response = MicrosoftApiResponse.ofResponse(networkResponse);

    // client code doesn't do this anymore, but it did at the time of regression.
    networkResponse.close();

    assertThat(response.isOkay()).isTrue();
    assertThrows(
        IOException.class,
        () -> {
          response.getJsonValue(
              objectMapper, "mykey", "trying to read bytes from a closed response body");
        });
  }

  private static Response.Builder fakeResponse(
      int statusCode, String httpMessage, String jsonBody) {
    Response.Builder builder = fakeResponse(statusCode, httpMessage);
    builder.body(ResponseBody.create(MediaType.parse("application/json"), jsonBody));
    return builder;
  }

  private static Response.Builder fakeResponse(int statusCode, String httpMessage) {
    Request fakeRequest = new Request.Builder().url("https://some/mock/url").build();
    return new Response.Builder()
        .request(fakeRequest)
        .protocol(HTTP_2)
        .code(statusCode)
        .message(httpMessage);
  }
}
