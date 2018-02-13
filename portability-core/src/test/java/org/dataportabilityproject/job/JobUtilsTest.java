package org.dataportabilityproject.job;

import static com.google.common.truth.Truth.assertThat;
import static org.dataportabilityproject.shared.ServiceMode.EXPORT;
import static org.dataportabilityproject.shared.ServiceMode.IMPORT;

import java.util.UUID;
import org.dataportabilityproject.shared.auth.PasswordAuthData;
import org.dataportabilityproject.shared.auth.TokenSecretAuthData;
import org.dataportabilityproject.spi.cloud.types.LegacyPortabilityJob;
import org.dataportabilityproject.types.transfer.auth.AuthData;
import org.junit.Test;

public class JobUtilsTest {
  @Test
  public void encodeDecodeRoundTrip() throws Exception {
    UUID jobId = UUID.randomUUID();
    assertThat(JobUtils.decodeJobId(JobUtils.encodeJobId(jobId))).isEqualTo(jobId);
  }

  @Test
  public void passwordAuthDataRoundTrip() throws Exception {
    LegacyPortabilityJob importJob = LegacyPortabilityJob.builder().build();
    AuthData authData = PasswordAuthData.create("myUsername", "myPassword");
    assertThat(JobUtils.getInitialAuthData(
        JobUtils.setInitialAuthData(importJob, authData, IMPORT), IMPORT)).isEqualTo(authData);

    LegacyPortabilityJob exportJob = LegacyPortabilityJob.builder().build();
    assertThat(JobUtils.getInitialAuthData(
        JobUtils.setInitialAuthData(exportJob, authData, EXPORT), EXPORT)).isEqualTo(authData);
  }

  @Test
  public void tokenAuthDataRoundTrip() throws Exception {
    LegacyPortabilityJob importJob = LegacyPortabilityJob.builder().build();
    AuthData authData = TokenSecretAuthData.create("myToken", "mySecret");
    assertThat(JobUtils.getInitialAuthData(
        JobUtils.setInitialAuthData(importJob, authData, IMPORT), IMPORT)).isEqualTo(authData);

    LegacyPortabilityJob exportJob = LegacyPortabilityJob.builder().build();
    assertThat(JobUtils.getInitialAuthData(
        JobUtils.setInitialAuthData(exportJob, authData, EXPORT), EXPORT)).isEqualTo(authData);
  }
}
