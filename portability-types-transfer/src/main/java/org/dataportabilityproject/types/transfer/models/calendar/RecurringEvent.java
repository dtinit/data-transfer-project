package org.dataportabilityproject.types.transfer.models.calendar;

import java.util.List;
import java.util.Map;

public class RecurringEvent {

  public class RRule {
    Freq freq;
    CalendarEventModel.CalendarEventTime until;
    int count;
    int interval;
    Map<ByRule, List<Property>> byRuleMap;
    Day wkst;

    public RRule(Freq freq, CalendarEventModel.CalendarEventTime until, int count, int interval,
          Map<ByRule, List<Property>> byRuleMap, Day wkst) {
      this.freq = freq;
      this.until = until;
      this.count = count;
      this.interval = interval;
      this.byRuleMap = byRuleMap;
      this.wkst = wkst;
    }
  }

  public class ExDate {
    List<CalendarEventModel.CalendarEventTime> exDateList;

    public ExDate(List<CalendarEventModel.CalendarEventTime> exDateList) {
      this.exDateList = exDateList;
    }
  }

  enum Freq {SECONDLY, MINUTELY, HOURLY, DAILY, WEEKLY, MONTHLY, YEARLY}

  enum ByRule {BYSECOND, BYMINUTE, BYHOUR, BYDAY, BYMONTHDAY, BYYEARDAY, BYWEEKNO, BYMONTH, BYSETPOS}

  enum Day {SU, MO, TU, WE, TH, FR, SA}
}
