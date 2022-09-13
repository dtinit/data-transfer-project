package org.datatransferproject.spi.transfer.provider;

import static org.datatransferproject.spi.transfer.provider.ImportResult.OK;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import org.datatransferproject.spi.transfer.provider.ImportResult.ResultType;
import org.junit.jupiter.api.Test;

public class ImportResultTest {

  @Test
  public void mergeOk() {
    assertEquals(OK, ImportResult.merge(OK, OK));
  }

  @Test
  public void mergeError() {
    ImportResult ir1 = new ImportResult(ResultType.ERROR);
    assertEquals(ir1, ImportResult.merge(ir1, OK));
    assertEquals(ir1, ImportResult.merge(OK, ir1));
  }

  @Test
  public void mergeCountsEmpty() {
    ImportResult ir1 = OK.copyWithCounts(Map.of("A", 10));
    assertEquals(ir1, ImportResult.merge(ir1, OK));
    assertEquals(ir1, ImportResult.merge(OK, ir1));
  }

  @Test
  public void mergeCounts() {
    ImportResult ir1 = OK.copyWithCounts(Map.of("A", 10, "B", 10));
    ImportResult ir2 = OK.copyWithCounts(Map.of("A", 5, "C", 5));
    ImportResult exp = OK.copyWithCounts(Map.of("A", 15, "B", 10, "C", 5));
    assertEquals(exp, ImportResult.merge(ir1, ir2));
  }

  @Test
  public void mergeBytes() {
    ImportResult ir1 = OK.copyWithBytes(7L);
    assertEquals(ir1, ImportResult.merge(ir1, OK));
    assertEquals(ir1, ImportResult.merge(OK, ir1));
    assertEquals(14L, ImportResult.merge(ir1, ir1).getBytes().get().longValue());
  }
}
