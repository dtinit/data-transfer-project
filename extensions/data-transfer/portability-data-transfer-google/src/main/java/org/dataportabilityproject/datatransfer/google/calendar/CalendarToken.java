package org.dataportabilityproject.datatransfer.google.calendar;

import org.dataportabilityproject.spi.transfer.types.StringPaginationToken;

public class CalendarToken extends StringPaginationToken {
  // A way to distinguish which kind of data a token retrieves

  /**
   * Ctor.
   *
   * @param token the token to get the next page
   */
  public CalendarToken(String token) {
    super(token);
  }
}
