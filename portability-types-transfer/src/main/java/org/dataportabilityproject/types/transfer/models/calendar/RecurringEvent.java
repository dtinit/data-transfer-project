package org.dataportabilityproject.types.transfer.models.calendar;

import java.util.List;
import java.util.Map;

public class RecurringEvent {

  enum Freq {
    SECONDLY,
    MINUTELY,
    HOURLY,
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY
  }

  enum ByRule {
    BYSECOND,
    BYMINUTE,
    BYHOUR,
    BYDAY,
    BYMONTHDAY,
    BYYEARDAY,
    BYWEEKNO,
    BYMONTH,
    BYSETPOS
  }

  enum Day {
    SU,
    MO,
    TU,
    WE,
    TH,
    FR,
    SA
  }

  enum TimeType {
    DATETIME("DATE-TIME"),
    DATE("DATE"),
    PERIOD("PERIOD"),
    UNDEFINED("UNDEFINED");

    private final String title;

    TimeType(String title) {
      this.title = title;
    }

    @Override
    public String toString() {
      return title;
    }
  }

  public class RRule {
    final Freq freq;
    // The "until" field could also just be a string, based on what's returned by the APIs
    final CalendarEventModel.CalendarEventTime until;
    final int count;
    final int interval;
    final Map<ByRule, List<Property>> byRuleMap;
    final Day wkst;

    public RRule(
        Freq freq,
        CalendarEventModel.CalendarEventTime until,
        int count,
        int interval,
        Map<ByRule, List<Property>> byRuleMap,
        Day wkst) {
      this.freq = freq;
      this.until = until;
      this.count = count;
      this.interval = interval;
      this.byRuleMap = byRuleMap;
      this.wkst = wkst;
    }
  }

  public class RDate {
    final TimeType value;
    // If we go with a string for "until" field, we should use a string here too
    final List<CalendarEventModel.CalendarEventTime> rdtparam;

    public RDate(TimeType value, List<CalendarEventModel.CalendarEventTime> rdtparam) {
      this.value = value;
      this.rdtparam = rdtparam;
    }

    public RDate(List<CalendarEventModel.CalendarEventTime> rdtparam) {
      this.value = TimeType.UNDEFINED;
      this.rdtparam = rdtparam;
    }

    // There is a lot of opportunity for sanity checking here, but as we will be getting extant
    // rdates, I'm not sure we need to do that yet.
  }

  public class ExDate {
    // If we go with a string for "until" field, we should use a string here too
    final List<CalendarEventModel.CalendarEventTime> exDateList;

    public ExDate(List<CalendarEventModel.CalendarEventTime> exDateList) {
      this.exDateList = exDateList;
    }
  }
}
