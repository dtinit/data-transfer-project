package org.dataportabilityproject.webapp;

import static com.google.common.truth.Truth.assertThat;
import static org.apache.axis.transport.http.HTTPConstants.HEADER_COOKIE;

import com.sun.net.httpserver.Headers;
import java.net.HttpCookie;
import java.util.Map;
import java.util.UUID;
import org.dataportabilityproject.job.JWTTokenManager;
import org.dataportabilityproject.job.JobUtils;
import org.dataportabilityproject.job.TokenManager;
import org.junit.Test;

public class PortabilityApiUtilsTest {
  private static final UUID JOB_ID = UUID.randomUUID();

  @Test
  public void parseCookieTest() {
    String cookieStr = "e_in=\"e_in_value\"; yummy_cookie=\"yummy_cookie_value\"";

    Headers httpHeaders = new Headers();
    httpHeaders.add(HEADER_COOKIE, cookieStr);
    Map<String, HttpCookie> cookies = PortabilityApiUtils.getCookies(httpHeaders);

    assertThat(cookies.containsKey("e_in")).isTrue();
    assertThat(cookies.get("e_in").getValue()).isEqualTo("e_in_value");

    assertThat(cookies.containsKey("yummy_cookie")).isTrue();
    assertThat(cookies.get("yummy_cookie")).isNotNull();
    assertThat(cookies.get("yummy_cookie").getValue()).isEqualTo("yummy_cookie_value");
  }

  @Test
  public void validateJobIdSuccessfulTest() {
    TokenManager tokenManager = new JWTTokenManager("TestSecret");

    // create cookies
    String encodedJobId = JobUtils.encodeJobId(JOB_ID);;
    String token = tokenManager.createNewToken(JOB_ID);
    String cookieStr = String
        .format("%s=%s;%s=%s", JsonKeys.ID_COOKIE_KEY, encodedJobId, JsonKeys.XSRF_TOKEN, token);

    // hook up cookies and token header
    Headers httpHeaders = new Headers();
    httpHeaders.add(HEADER_COOKIE, cookieStr);
    httpHeaders.add(JsonKeys.XSRF_HEADER, token);

    UUID jobId = PortabilityApiUtils.validateJobId(httpHeaders, tokenManager);
    assertThat(jobId).isEqualTo(JOB_ID);
  }

  @Test(expected = IllegalArgumentException.class)
  public void validateJobIdMismatchTest(){
    TokenManager tokenManager = new JWTTokenManager("TestSecret");

    // create cookies - purposefully create a token thats not for the correct job ID.
    String encodedJobId = JobUtils.encodeJobId(JOB_ID);
    UUID anotherId = UUID.randomUUID();
    while (anotherId.equals(JOB_ID)) {
      anotherId = UUID.randomUUID();
    }
    String token = tokenManager.createNewToken(anotherId);

    String cookieStr = String
        .format("%s=%s;%s=%s", JsonKeys.ID_COOKIE_KEY, encodedJobId, JsonKeys.XSRF_TOKEN, token);

    // hook up cookies and token header
    Headers httpHeaders = new Headers();
    httpHeaders.add(HEADER_COOKIE, cookieStr);
    httpHeaders.add(JsonKeys.XSRF_HEADER, token);

    // Should throw IllegalArgumentException due to mismatch of token and encoded job id.
    PortabilityApiUtils.validateJobId(httpHeaders, tokenManager);
  }

  @Test(expected = IllegalArgumentException.class)
  public void validateJobIdMissingCookieTest(){
    TokenManager tokenManager = new JWTTokenManager("TestSecret");

    // create cookies
    String encodedJobId = JobUtils.encodeJobId(JOB_ID);;
    String token = tokenManager.createNewToken(JOB_ID);
    String cookieStr = String.format("%s=%s;", JsonKeys.ID_COOKIE_KEY, encodedJobId);

    // hook up cookies and token header
    Headers httpHeaders = new Headers();
    httpHeaders.add(HEADER_COOKIE, cookieStr);
    httpHeaders.add(JsonKeys.XSRF_HEADER, token);

    // this should throw IllegalArgumentException due to missing XSRF_TOKEN cookie
    PortabilityApiUtils.validateJobId(httpHeaders, tokenManager);
  }

  @Test(expected = NullPointerException.class)
  public void validateJobIdMissingHeaderTest(){
    TokenManager tokenManager = new JWTTokenManager("TestSecret");

    // create cookies
    String encodedJobId = JobUtils.encodeJobId(JOB_ID);
    String token = tokenManager.createNewToken(JOB_ID);
    String cookieStr = String
        .format("%s=%s;%s=%s", JsonKeys.ID_COOKIE_KEY, encodedJobId, JsonKeys.XSRF_TOKEN, token);

    // hook up cookies and token header
    Headers httpHeaders = new Headers();
    httpHeaders.add(HEADER_COOKIE, cookieStr);

    // this should throw NullPointerException due to missing X-XSRF-TOKEN header
    PortabilityApiUtils.validateJobId(httpHeaders, tokenManager);
  }
}
