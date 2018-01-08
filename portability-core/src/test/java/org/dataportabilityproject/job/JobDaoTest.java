package org.dataportabilityproject.job;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

import org.dataportabilityproject.cloud.interfaces.PersistentKeyValueStore;
import org.dataportabilityproject.cloud.local.LocalCloudFactory;
import org.dataportabilityproject.job.JobDao.JobState;
import org.dataportabilityproject.shared.PortableDataType;
import org.junit.Before;
import org.junit.Test;

public class JobDaoTest {
  private static final String TEST_JOB_ID = "test-job-id";
  private static final String TEST_EXPORT_SERVICE = "test-export-service";
  private static final String TEST_IMPORT_SERVICE = "test-import-service";
  private PersistentKeyValueStore keyValueStore;
  private JobDao jobDao;

  @Before
  public void setUp() {
    jobDao = new JobDao(new LocalCloudFactory().getPersistentKeyValueStore());
  }

  @Test
  public void testUpdatePendingAuthDataJob() throws Exception {
    PortabilityJob job = createTestJob();
    assertThat(jobDao.lookupJobPendingAuthData(TEST_JOB_ID)).isNull();
    jobDao.insertJobInPendingAuthDataState(job);
    job = jobDao.lookupJobPendingAuthData(TEST_JOB_ID);
    assertThat(job.id()).isEqualTo(TEST_JOB_ID);
    jobDao.updatePendingAuthDataJob(
        job.toBuilder().setEncryptedExportAuthData("enc-import-data").build());
    job = jobDao.lookupJobPendingAuthData(TEST_JOB_ID);
    assertThat(job.encryptedExportAuthData()).isEqualTo("enc-import-data");
    jobDao.deleteJob(TEST_JOB_ID, JobState.PENDING_AUTH_DATA);
  }

  @Test
  public void testUpdatePendingAuthDataJob_doesNotExist() throws Exception {
    assertThat(jobDao.lookupJobPendingAuthData(TEST_JOB_ID)).isNull();
    try {
      jobDao.updatePendingAuthDataJob(createTestJob());
      fail();
    } catch (IllegalArgumentException e) {
      // expected
    }
  }

  private static PortabilityJob createTestJob() {
    return PortabilityJob.builder()
        .setId(TEST_JOB_ID)
        .setDataType(PortableDataType.TASKS.name())
        .setExportService(TEST_EXPORT_SERVICE)
        .setImportService(TEST_IMPORT_SERVICE)
        .build();
  }
}
