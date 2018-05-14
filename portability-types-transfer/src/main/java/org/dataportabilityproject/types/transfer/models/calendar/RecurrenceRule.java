package org.dataportabilityproject.types.transfer.models.calendar;


import java.util.LinkedList;
import java.util.List;
import net.fortuna.ical4j.model.property.ExDate;
import net.fortuna.ical4j.model.property.RDate;
import net.fortuna.ical4j.model.property.RRule;

public class RecurrenceRule {

  public static final String RRULE = "RRULE";
  public static final String RDATE = "RDATE";
  public static final String EXDATE = "EXDATE";

  private RRule rRule;
  private RDate rDate;
  private ExDate exDate;

  public RRule getrRule() {
    return rRule;
  }

  public RDate getrDate() {
    return rDate;
  }

  public ExDate getExDate() {
    return exDate;
  }

  public List<String> getStringList() {
    List<String> stringList = new LinkedList<>();
    if (rRule != null) {
      stringList.add(rRule.toString());
    }
    if (rDate != null) {
      stringList.add(rDate.toString());
    }
    if (exDate != null) {
      stringList.add(exDate.toString());
    }
  }

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

  /*
  public static RRule parseRRuleString(String rRuleString) {
    List<String> components = Arrays.asList(rRuleString.split(";"));
    return null;
  }

  public static RDate parseRDateString(String rDateString) {
    return null;
  }

  public static ExDate parseExDateString(String exDateString) {
    return null;
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
  */

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
