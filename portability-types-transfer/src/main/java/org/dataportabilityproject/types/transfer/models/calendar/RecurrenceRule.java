package org.dataportabilityproject.types.transfer.models.calendar;

import com.google.common.base.Preconditions;
import java.util.List;
import java.util.Map;

public class RecurrenceRule {

  private RRule rRule;
  private RDate rDate;
  private ExDate exDate;

  private RecurrenceRule(RRule rRule, RDate rDate, ExDate exDate) {
    this.rRule = rRule;
    this.rDate = rDate;
    this.exDate = exDate;
  }

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

  public static class RRule {
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

  public static class Builder {
    private RRule rRule;
    private RDate rDate;
    private ExDate exDate;

    public Builder() {}

    public Builder setRRule(RRule rRule) {
      this.rRule = rRule;
      return this;
    }

    public Builder setRDate(RDate rDate) {
      this.rDate = rDate;
      return this;
    }

    public Builder setExDate(ExDate exDate) {
      this.exDate = exDate;
      return this;
    }

    public RecurrenceRule build() {
      return new RecurrenceRule(rRule, rDate, exDate);
    }
  }
}
