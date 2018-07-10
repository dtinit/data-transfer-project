package org.datatransferproject.auth.google;

import com.google.api.client.http.HttpTransport;
import com.google.api.services.calendar.CalendarScopes;
import org.datatransferproject.spi.api.auth.AuthServiceProviderRegistry.AuthMode;
import org.datatransferproject.spi.api.types.AuthFlowConfiguration;
import org.datatransferproject.types.transfer.auth.AppCredentials;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

/** Initial test for GoogleAuthDataGenerator. */
@RunWith(MockitoJUnitRunner.class)
public class GoogleAuthDataGeneratorTest {
  AppCredentials appCredentials = new AppCredentials("dummy-id", "dummy-secret");
  @Mock private HttpTransport httpTransport;

  @Test
  public void generateConfigurationImport() {
    GoogleAuthDataGenerator generator =
        new GoogleAuthDataGenerator(
            "redirect", appCredentials, httpTransport, null, "calendar", AuthMode.IMPORT);

    AuthFlowConfiguration config =
        generator.generateConfiguration("http://localhost/test", "54321");

    Assert.assertEquals(
        "https://accounts.google.com/o/oauth2/auth?"
            + "access_type=offline&approval_prompt=force&client_id=dummy-id"
            + "&redirect_uri=http://localhost/testredirect&response_type=code"
            + "&scope="
            + CalendarScopes.CALENDAR
            + "&state=NTQzMjE%3D",
        config.getUrl());
  }

  @Test
  public void generateConfigurationExport() {
    GoogleAuthDataGenerator generator =
        new GoogleAuthDataGenerator(
            "redirect", appCredentials, httpTransport, null, "calendar", AuthMode.EXPORT);

    AuthFlowConfiguration config =
        generator.generateConfiguration("http://localhost/test", "54321");

    Assert.assertEquals(
        "https://accounts.google.com/o/oauth2/auth?"
            + "access_type=offline&approval_prompt=force&client_id=dummy-id"
            + "&redirect_uri=http://localhost/testredirect&response_type=code"
            + "&scope="
            + CalendarScopes.CALENDAR_READONLY
            + "&state=NTQzMjE%3D",
        config.getUrl());
  }
}
