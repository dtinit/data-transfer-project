package org.dataportabilityproject.auth.google;

import com.google.api.client.http.HttpTransport;
import org.dataportabilityproject.spi.gateway.types.AuthFlowConfiguration;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

/** Initial test for GoogleAuthDataGenerator. */
@RunWith(MockitoJUnitRunner.class)
public class GoogleAuthDataGeneratorTest {
  @Mock private HttpTransport httpTransport;

  @Test
  public void generateConfiguration() {
    GoogleAuthDataGenerator generator =
        new GoogleAuthDataGenerator("redirect", "dummy-id", "dummy-secret", httpTransport, null);
    AuthFlowConfiguration config =
        generator.generateConfiguration("http://localhost/test", "54321");
    Assert.assertEquals(
        "https://accounts.google.com/o/oauth2/auth?"
            + "access_type=offline&approval_prompt=force&client_id=dummy-id"
            + "&redirect_uri=http://localhost/testredirect&response_type=code"
            + "&scope=user.read%20mail.read%20Contacts.ReadWrite%20Calendars.ReadWrite&state=NTQzMjE%3D",
        config.getUrl());
  }
}
