package org.dataportabilityproject.job;

import static com.google.common.truth.Truth.assertThat;
import static org.dataportabilityproject.shared.ServiceMode.EXPORT;
import static org.dataportabilityproject.shared.ServiceMode.IMPORT;

import org.dataportabilityproject.shared.ServiceMode;
import org.dataportabilityproject.shared.auth.AuthData;
import org.dataportabilityproject.shared.auth.PasswordAuthData;
import org.dataportabilityproject.shared.auth.TokenSecretAuthData;
import org.junit.Test;

public class JobUtilsTest {
  @Test
  public void encodeDecodeRoundTrip() throws Exception {
    String jobId = "This is my @$*(#$ job id \t\n";
    PortabilityJob job = PortabilityJob.builder()
        .setId(jobId)
        .build();
    assertThat(JobUtils.decodeId(JobUtils.encodeId(job))).isEqualTo(jobId);
  }

  @Test
  public void passwordAuthDataRoundTrip() throws Exception {
    PortabilityJob importJob = PortabilityJob.builder().setId("id").build();
    AuthData authData = PasswordAuthData.create("myUsername", "myPassword");
    assertThat(JobUtils.getInitialAuthData(
        JobUtils.setInitialAuthData(importJob, authData, IMPORT), IMPORT)).isEqualTo(authData);

    PortabilityJob exportJob = PortabilityJob.builder().setId("id").build();
    assertThat(JobUtils.getInitialAuthData(
        JobUtils.setInitialAuthData(exportJob, authData, EXPORT), EXPORT)).isEqualTo(authData);
  }

  @Test
  public void tokenAuthDataRoundTrip() throws Exception {
    PortabilityJob importJob = PortabilityJob.builder().setId("id").build();
    AuthData authData = TokenSecretAuthData.create("myToken", "mySecret");
    assertThat(JobUtils.getInitialAuthData(
        JobUtils.setInitialAuthData(importJob, authData, IMPORT), IMPORT)).isEqualTo(authData);

    PortabilityJob exportJob = PortabilityJob.builder().setId("id").build();
    assertThat(JobUtils.getInitialAuthData(
        JobUtils.setInitialAuthData(exportJob, authData, EXPORT), EXPORT)).isEqualTo(authData);
  }
}
