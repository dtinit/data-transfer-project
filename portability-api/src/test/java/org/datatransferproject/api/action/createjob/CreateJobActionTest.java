package org.datatransferproject.api.action.createjob;

import org.datatransferproject.security.SymmetricKeyGenerator;
import org.datatransferproject.spi.cloud.storage.JobStore;
import org.junit.Assert;
import org.junit.Test;

import javax.crypto.SecretKey;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CreateJobActionTest {

  @Test
  public void testGetRequestType() {
    JobStore store = mock(JobStore.class);
    SymmetricKeyGenerator symmetricKeyGenerator = mock(SymmetricKeyGenerator.class);
    CreateJobAction createJobAction = new CreateJobAction(store, symmetricKeyGenerator);

    Class<CreateJobActionRequest> actual = createJobAction.getRequestType();
    Assert.assertEquals(actual, CreateJobActionRequest.class);
  }

  @Test
  public void testHandle() {
    JobStore store = mock(JobStore.class);

    SecretKey secretKey = mock(SecretKey.class);
    when(secretKey.getEncoded()).thenReturn(new byte[] {'a','b'});

    SymmetricKeyGenerator symmetricKeyGenerator = mock(SymmetricKeyGenerator.class);
    when(symmetricKeyGenerator.generate()).thenReturn(secretKey);

    CreateJobAction createJobAction = new CreateJobAction(store, symmetricKeyGenerator);

    CreateJobActionRequest request = mock(CreateJobActionRequest.class);
    when(request.getTransferDataType()).thenReturn("contacts");
    when(request.getSource()).thenReturn("Source");
    when(request.getDestination()).thenReturn("Destination");

    CreateJobActionResponse actual = createJobAction.handle(request);
    Assert.assertNotNull(actual);
    Assert.assertNotNull(actual.getId());
  }
}
