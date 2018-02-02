package org.dataportabilityproject.job;

import static com.google.common.truth.Truth.assertThat;
import static org.dataportabilityproject.shared.ServiceMode.EXPORT;
import static org.dataportabilityproject.shared.ServiceMode.IMPORT;

import org.dataportabilityproject.shared.auth.PasswordAuthData;
import org.dataportabilityproject.shared.auth.TokenSecretAuthData;
import org.dataportabilityproject.spi.cloud.types.OldPortabilityJob;
import org.dataportabilityproject.types.transfer.auth.AuthData;
import org.junit.Test;

public class JobUtilsTest {
  @Test
  public void encodeDecodeRoundTrip() throws Exception {
    String jobId = "This is my @$*(#$ job id \t\n";
    OldPortabilityJob job = OldPortabilityJob.builder()
        .setId(jobId)
        .build();
    assertThat(JobUtils.decodeId(JobUtils.encodeId(job))).isEqualTo(jobId);
  }

  @Test
  public void passwordAuthDataRoundTrip() throws Exception {
    OldPortabilityJob importJob = OldPortabilityJob.builder().setId("id").build();
    AuthData authData = PasswordAuthData.create("myUsername", "myPassword");
    assertThat(JobUtils.getInitialAuthData(
        JobUtils.setInitialAuthData(importJob, authData, IMPORT), IMPORT)).isEqualTo(authData);

    OldPortabilityJob exportJob = OldPortabilityJob.builder().setId("id").build();
    assertThat(JobUtils.getInitialAuthData(
        JobUtils.setInitialAuthData(exportJob, authData, EXPORT), EXPORT)).isEqualTo(authData);
  }

  @Test
  public void tokenAuthDataRoundTrip() throws Exception {
    OldPortabilityJob importJob = OldPortabilityJob.builder().setId("id").build();
    AuthData authData = TokenSecretAuthData.create("myToken", "mySecret");
    assertThat(JobUtils.getInitialAuthData(
        JobUtils.setInitialAuthData(importJob, authData, IMPORT), IMPORT)).isEqualTo(authData);

    OldPortabilityJob exportJob = OldPortabilityJob.builder().setId("id").build();
    assertThat(JobUtils.getInitialAuthData(
        JobUtils.setInitialAuthData(exportJob, authData, EXPORT), EXPORT)).isEqualTo(authData);
  }
}
