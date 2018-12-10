package org.datatransferproject.api.action.datatype;

import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.spi.api.auth.AuthServiceProviderRegistry;
import org.datatransferproject.types.client.datatype.DataTypes;
import org.datatransferproject.types.client.datatype.GetDataTypes;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DataTypesActionTest {

  @Test
  public void testGetRequestType() {
    AuthServiceProviderRegistry registry = mock(AuthServiceProviderRegistry.class);
    DataTypesAction dataTypesAction = new DataTypesAction(registry, new Monitor() {});

    Class<GetDataTypes> actual = dataTypesAction.getRequestType();
    Assert.assertNotEquals(actual, null);
    Assert.assertEquals(actual, GetDataTypes.class);
  }

  @Test
  public void testHandle() {
    AuthServiceProviderRegistry registry = mock(AuthServiceProviderRegistry.class);
    Set<String> dataTypes = new HashSet<>(Arrays.asList("CONTACTS", "PHOTOS"));
    when(registry.getTransferDataTypes()).thenReturn(dataTypes);
    DataTypesAction dataTypesAction = new DataTypesAction(registry, new Monitor() {});

    GetDataTypes request = mock(GetDataTypes.class);
    DataTypes actual = dataTypesAction.handle(request);
    Assert.assertEquals(actual.getDataTypes(), dataTypes);
  }
}
