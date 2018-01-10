package org.dataportabilityproject.webapp;

import static com.google.common.truth.Truth.assertThat;
import static org.apache.axis.transport.http.HTTPConstants.HEADER_COOKIE;

import com.sun.net.httpserver.Headers;
import org.dataportabilityproject.job.JWTTokenManager;
import org.dataportabilityproject.job.JobUtils;
import org.dataportabilityproject.job.PortabilityJob;
import org.dataportabilityproject.job.TokenManager;
import org.junit.Before;
import org.junit.Test;

public class StartCopyHandlerTest {
  private TokenManager tokenManager;
  private StartCopyHandler startCopyHandler;

  @Before
  public void setup(){
    tokenManager = new JWTTokenManager("TestSecret");
    startCopyHandler = new StartCopyHandler(tokenManager);
  }

  @Test
  public void validateJobIdTest() {
    PortabilityJob testJob = PortabilityJob.builder().setId("123456").build();

    // create cookies
    String encodedJobId = JobUtils.encodeId(testJob);;
    String token = tokenManager.createNewToken(testJob.id());
    String cookieStr = String
        .format("%s=%s;%s=%s", JsonKeys.ID_COOKIE_KEY, encodedJobId, JsonKeys.XSRF_TOKEN, token);

    // hook up cookies and token header
    Headers httpHeaders = new Headers();
    httpHeaders.add(HEADER_COOKIE, cookieStr);
    httpHeaders.add(JsonKeys.XSRF_HEADER, token);

    String jobId = startCopyHandler.validateJobId(httpHeaders);
    assertThat(jobId).isEqualTo(testJob.id());
  }

  @Test(expected = IllegalArgumentException.class)
  public void mismatchJobIdTest(){
    PortabilityJob testJob = PortabilityJob.builder().setId("123456").build();

    // create cookies - purposefully create a token thats not for the correct job ID.
    String encodedJobId = JobUtils.encodeId(testJob);;
    String token = tokenManager.createNewToken("789");

    String cookieStr = String
        .format("%s=%s;%s=%s", JsonKeys.ID_COOKIE_KEY, encodedJobId, JsonKeys.XSRF_TOKEN, token);

    // hook up cookies and token header
    Headers httpHeaders = new Headers();
    httpHeaders.add(HEADER_COOKIE, cookieStr);
    httpHeaders.add(JsonKeys.XSRF_HEADER, token);

    // Should throw IllegalArgumentException due to mismatch of token and encoded job id.
    String jobId = startCopyHandler.validateJobId(httpHeaders);
  }

  @Test(expected = IllegalArgumentException.class)
  public void missingCookieTest(){
    PortabilityJob testJob = PortabilityJob.builder().setId("123456").build();

    // create cookies
    String encodedJobId = JobUtils.encodeId(testJob);;
    String token = tokenManager.createNewToken(testJob.id());
    String cookieStr = String
        .format("%s=%s;", JsonKeys.ID_COOKIE_KEY, encodedJobId);

    // hook up cookies and token header
    Headers httpHeaders = new Headers();
    httpHeaders.add(HEADER_COOKIE, cookieStr);
    httpHeaders.add(JsonKeys.XSRF_HEADER, token);

    // this should throw IllegalArgumentException due to missing XSRF_TOKEN cookie
    String jobId = startCopyHandler.validateJobId(httpHeaders);
  }

  @Test(expected = NullPointerException.class)
  public void missingHeaderTest(){
    PortabilityJob testJob = PortabilityJob.builder().setId("123456").build();

    // create cookies
    String encodedJobId = JobUtils.encodeId(testJob);;
    String token = tokenManager.createNewToken(testJob.id());
    String cookieStr = String
        .format("%s=%s;%s=%s", JsonKeys.ID_COOKIE_KEY, encodedJobId, JsonKeys.XSRF_TOKEN, token);

    // hook up cookies and token header
    Headers httpHeaders = new Headers();
    httpHeaders.add(HEADER_COOKIE, cookieStr);

    // this should throw NullPointerException due to missing X-XSRF-TOKEN header
    String jobId = startCopyHandler.validateJobId(httpHeaders);
  }
}
