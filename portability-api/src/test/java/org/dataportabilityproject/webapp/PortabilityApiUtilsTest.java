package org.dataportabilityproject.webapp;

import static org.apache.axis.transport.http.HTTPConstants.HEADER_COOKIE;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertTrue;

import com.sun.net.httpserver.Headers;
import java.net.HttpCookie;
import java.util.Map;
import org.junit.Test;

public class PortabilityApiUtilsTest {
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
}
