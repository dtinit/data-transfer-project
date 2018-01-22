/*
 * Copyright 2017 The Data-Portability Project Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.dataportabilityproject.job;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

import org.dataportabilityproject.cloud.interfaces.PersistentKeyValueStore;
import org.dataportabilityproject.cloud.local.InMemoryPersistentKeyValueStore;
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
    jobDao = new JobDao(new InMemoryPersistentKeyValueStore());
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
