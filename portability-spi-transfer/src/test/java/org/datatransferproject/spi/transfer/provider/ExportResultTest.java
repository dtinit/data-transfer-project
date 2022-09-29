package org.datatransferproject.spi.transfer.provider;

import static org.datatransferproject.spi.transfer.provider.ExportResult.ResultType.CONTINUE;
import static org.datatransferproject.spi.transfer.provider.ExportResult.ResultType.END;
import static org.datatransferproject.spi.transfer.provider.ExportResult.ResultType.ERROR;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.datatransferproject.spi.transfer.provider.ExportResult.ResultType;
import org.junit.jupiter.api.Test;

public class ExportResultTest {

  @Test
  public void resultTypeMerge() {
    assertEquals(ERROR, ResultType.merge(ERROR, END));
    assertEquals(ERROR, ResultType.merge(END, ERROR));
    assertEquals(CONTINUE, ResultType.merge(END, CONTINUE));
    assertEquals(CONTINUE, ResultType.merge(CONTINUE, END));
    assertEquals(END, ResultType.merge(END, END));
  }

}
