package org.datatransferproject.cloud.google;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.Transaction;
import com.google.cloud.datastore.testing.LocalDatastoreHelper;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.types.transfer.errors.ErrorDetail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.UUID;



public class GoogleCloudIdempotentImportExecutorTest {

  private static final String ITEM_NAME = "item1";
  private static final Monitor monitor = Mockito.mock(Monitor.class);
  private static final UUID JOB_ID = UUID.randomUUID();
  private static final UUID JOB_ID_2 = UUID.randomUUID();

  private static LocalDatastoreHelper localDatastoreHelper;
  private static Datastore datastore;
  private static GoogleCloudIdempotentImportExecutor googleExecutor;

  @BeforeEach
  public void setUp() throws IOException, InterruptedException {
    // create a local datastore with 100% consistency for testing. This is ok to assume for the
    // purposes of this test because the implementation only reads from datastore after a job has
    // been restarted
    localDatastoreHelper = LocalDatastoreHelper.create(1.0);
    localDatastoreHelper.start();
    datastore = localDatastoreHelper.getOptions().getService();
    googleExecutor = new GoogleCloudIdempotentImportExecutor(datastore, monitor);

  }

  @Test
  public void getCachedValue() throws Exception {
    googleExecutor.setJobId(JOB_ID);
    googleExecutor.executeAndSwallowIOExceptions("id1", ITEM_NAME, () -> "idempotentId1");
    assertEquals(googleExecutor.getCachedValue("id1"), "idempotentId1");
  }

  @Test
  public void loadCachedValuesFromDataStore() throws Exception {
    initializeDS();
    googleExecutor.setJobId(JOB_ID);
    assertEquals(googleExecutor.getCachedValue("id1"), "idempotentId1");
    assertEquals(googleExecutor.getCachedValue("id2"), "idempotentId2");
    assertEquals(googleExecutor.getCachedValue("id3"), "idempotentId3");
    assertEquals(googleExecutor.getErrors().size(), 1);
    assertTrue(googleExecutor.getErrors().contains(
        ErrorDetail.builder().setId("id4").setTitle("title").setException("error").build()));

    // we shouldn't load any items belonging to JOB_ID_2
    assertFalse(googleExecutor.isKeyCached("id1_job2"));
  }

  @Test
  public void removeErrorIfItemSucceeds() throws Exception {
    initializeDS();
    googleExecutor.setJobId(JOB_ID);
    assertTrue(googleExecutor.getErrors().contains(
        ErrorDetail.builder().setId("id4").setTitle("title").setException("error").build()));

    // now execute a successful import of id4
    googleExecutor.executeAndSwallowIOExceptions("id4", ITEM_NAME, () -> "idempotentId4");
    assertEquals(googleExecutor.getCachedValue("id4"), "idempotentId4");
    assertEquals(googleExecutor.getErrors().size(), 0);

    // reset the jobId to trigger another read from datastore
    googleExecutor.setJobId(JOB_ID);
    assertEquals(googleExecutor.getErrors().size(), 0);
  }

  private void initializeDS() throws IOException {
    Transaction t = datastore.newTransaction();
    t.put(googleExecutor.createResultEntity("id1", JOB_ID, "idempotentId1"));
    t.put(googleExecutor.createResultEntity("id2", JOB_ID, "idempotentId2"));
    t.put(googleExecutor.createResultEntity("id3", JOB_ID, "idempotentId3"));
    t.put(googleExecutor.createErrorEntity("id4", JOB_ID,
        ErrorDetail.builder().setId("id4").setTitle("title").setException("error").build()));

    t.put(googleExecutor.createResultEntity("id1_job2", JOB_ID_2, "idempotentId1_job2"));
    t.commit();
  }
}
