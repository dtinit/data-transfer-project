package org.datatransferproject.api.action.datatype;

import static org.datatransferproject.types.common.models.DataVertical.CONTACTS;
import static org.datatransferproject.types.common.models.DataVertical.PHOTOS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.spi.api.auth.AuthServiceProviderRegistry;
import org.datatransferproject.types.client.datatype.DataTypes;
import org.datatransferproject.types.client.datatype.GetDataTypes;
import org.datatransferproject.types.common.models.DataVertical;
import org.junit.jupiter.api.Test;

public class DataTypesActionTest {

  @Test
  public void testGetRequestType() {
    AuthServiceProviderRegistry registry = mock(AuthServiceProviderRegistry.class);
    DataTypesAction dataTypesAction = new DataTypesAction(registry, new Monitor() {
    });

    Class<GetDataTypes> actual = dataTypesAction.getRequestType();
    assertNotEquals(actual, null);
    assertEquals(actual, GetDataTypes.class);
  }

  @Test
  public void testHandle() {
    AuthServiceProviderRegistry registry = mock(AuthServiceProviderRegistry.class);
    Set<DataVertical> dataTypes = new HashSet<>(Arrays.asList(CONTACTS, PHOTOS));
    when(registry.getTransferDataTypes()).thenReturn(dataTypes);
    DataTypesAction dataTypesAction = new DataTypesAction(registry, new Monitor() {
    });

    GetDataTypes request = mock(GetDataTypes.class);
    DataTypes actual = dataTypesAction.handle(request);
    assertEquals(actual.getDataTypes(), dataTypes);
  }
}
