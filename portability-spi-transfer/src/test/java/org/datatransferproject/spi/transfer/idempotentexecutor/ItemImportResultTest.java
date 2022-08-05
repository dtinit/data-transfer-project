package org.datatransferproject.spi.transfer.idempotentexecutor;

import static org.datatransferproject.spi.transfer.idempotentexecutor.ItemImportResult.Status.SUCCESS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;
import org.junit.jupiter.api.Assertions;

public class ItemImportResultTest {

  @Test
  public void testNullBytesIsOk() {
    ItemImportResult<String> result = ItemImportResult.success("blabla", null);
    assertEquals(SUCCESS, result.getStatus());
    assertFalse(result.hasBytes());
  }

  @Test
  public void testNullBytesThrowOnGet() {
    Assertions.assertThrows(IllegalStateException.class, () -> {
      ItemImportResult.success("blabla", null).getBytes();
    });
  }

  @Test
  public void testSuccessWithNoData() {
    Assertions.assertThrows(NullPointerException.class, () -> {
      ItemImportResult.success(null, 0L);
    });
  }

  @Test
  public void testFailWithIncorrectBytes() {
    Assertions.assertThrows(IllegalArgumentException.class, () -> {
      ItemImportResult.success("blabla", -1L);
    });
  }

  @Test
  public void testErrorWithNoException() {
    Assertions.assertThrows(NullPointerException.class, () -> {
      ItemImportResult.error(null, 10L);
    });
  }
}
