package org.datatransferproject.spi.transfer.idempotentexecutor;

import static org.datatransferproject.spi.transfer.idempotentexecutor.ItemImportResult.Status.SUCCESS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.Test;

public class ItemImportResultTest {

  @Test
  public void testNullBytesIsOk() {
    ItemImportResult<String> result = ItemImportResult.success("blabla");
    assertEquals(SUCCESS, result.getStatus());
    assertFalse(result.hasBytes());
  }

  @Test
  public void testNullBytesThrowOnGet() {
    assertThrows(IllegalStateException.class, () -> {
      ItemImportResult.success("blabla", null).getBytes();
    });
  }

  @Test
  public void testSuccessWithNoData() {
    assertThrows(NullPointerException.class, () -> {
      ItemImportResult.success(null, 0L);
    });
  }

  @Test
  public void testFailWithIncorrectBytes() {
    assertThrows(IllegalArgumentException.class, () -> {
      ItemImportResult.success("blabla", -1L);
    });
  }

  @Test
  public void testErrorWithNoException() {
    assertThrows(NullPointerException.class, () -> {
      ItemImportResult.error(null, 10L);
    });
  }
}
