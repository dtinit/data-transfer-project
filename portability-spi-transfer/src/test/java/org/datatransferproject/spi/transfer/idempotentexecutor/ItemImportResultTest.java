package org.datatransferproject.spi.transfer.idempotentexecutor;

import static org.datatransferproject.spi.transfer.idempotentexecutor.ItemImportResult.Status.SUCCESS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class ItemImportResultTest {

  @Test
  public void testNullBytesIsOk() {
    ItemImportResult<String> result = ItemImportResult.success("blabla", null);
    assertEquals(SUCCESS, result.getStatus());
    assertFalse(result.hasBytes());
  }

  @Test(expected = IllegalStateException.class)
  public void testNullBytesThrowOnGet() {
    ItemImportResult.success("blabla", null).getBytes();
  }

  @Test(expected = NullPointerException.class)
  public void testSuccessWithNoData() {
    ItemImportResult.success(null, 0L);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testFailWithIncorrectBytes() {
    ItemImportResult.success("blabla", -1L);
  }

  @Test(expected = NullPointerException.class)
  public void testErrorWithNoException() {
    ItemImportResult.error(null, 10L);
  }
}
