package org.dataportabilityproject.types.transfer.models.calendar;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class RecurringEvent {

  public class RRule {
    Freq freq;
    // The "until" field could also just be a string, based on what's returned by the APIs
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

  public class RDate {
    Optional<TimeType> value;
    // If we go with a string for "until" field, we should use a string here too
    List<CalendarEventModel.CalendarEventTime> rdtparam;

    public RDate(Optional<TimeType> value, List<CalendarEventModel.CalendarEventTime> rdtparam) {
      this.value = value;
      this.rdtparam = rdtparam;
    }

    public RDate(List<CalendarEventModel.CalendarEventTime> rdtparam) {
      this.value = Optional.empty();
      this.rdtparam = rdtparam;
    }

    // There is a lot of opportunity for sanity checking here, but as we will be getting extant
    // rdates, I'm not sure we need to do that yet.
  }

  public class ExDate {
    // If we go with a string for "until" field, we should use a string here too
    List<CalendarEventModel.CalendarEventTime> exDateList;

    public ExDate(List<CalendarEventModel.CalendarEventTime> exDateList) {
      this.exDateList = exDateList;
    }
  }

  enum Freq {SECONDLY, MINUTELY, HOURLY, DAILY, WEEKLY, MONTHLY, YEARLY}

  enum ByRule {BYSECOND, BYMINUTE, BYHOUR, BYDAY, BYMONTHDAY, BYYEARDAY, BYWEEKNO, BYMONTH, BYSETPOS}

  enum Day {SU, MO, TU, WE, TH, FR, SA}

  enum TimeType {
    DATETIME("DATE-TIME"),
    DATE("DATE"),
    PERIOD("PERIOD");

    private String title;

    TimeType(String title) {
      this.title = title;
    }

    @Override
    public String toString() {
      return title;
    }
  }
}
