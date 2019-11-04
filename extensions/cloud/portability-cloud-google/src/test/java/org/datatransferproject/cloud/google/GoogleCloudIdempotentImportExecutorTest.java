package org.datatransferproject.cloud.google;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.Transaction;
import com.google.cloud.datastore.testing.LocalDatastoreHelper;
import java.io.IOException;
import java.util.UUID;
import jdk.nashorn.internal.scripts.JO;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.types.transfer.errors.ErrorDetail;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class GoogleCloudIdempotentImportExecutorTest {

  private static final String ITEM_NAME = "item1";
  private static final Monitor monitor = Mockito.mock(Monitor.class);

  private static final UUID JOB_ID = UUID.randomUUID();
  private static LocalDatastoreHelper localDatastoreHelper;
  private static Datastore datastore;
  private static GoogleTempFileStore tempFileStore = Mockito.mock(GoogleTempFileStore.class);
  private static GoogleCloudIdempotentImportExecutor googleExecutor;

  @Before
  public void setUp() throws IOException, InterruptedException {
    localDatastoreHelper = LocalDatastoreHelper.create();
    localDatastoreHelper.start();
    System.setProperty("DATASTORE_EMULATOR_HOST", "localhost:" + localDatastoreHelper.getPort());

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
    t.commit();
  }
}
