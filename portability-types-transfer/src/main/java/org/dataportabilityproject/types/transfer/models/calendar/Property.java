package org.dataportabilityproject.types.transfer.models.calendar;

// This interface could be used by other models.  If so, we should move it out and make it more
// broadly useful.
interface Property<T> {
  T getValue();
}

final class RuleDay implements Property<RecurringEvent.Day> {
  private RecurringEvent.Day day;

  RuleDay(RecurringEvent.Day day) {
    this.day = day;
  }

  @Override
  public RecurringEvent.Day getValue() {
    return day;
  }
}

final class RuleNumber implements Property<Integer> {
  private Integer number;

  RuleNumber(Integer number) {
    this.number = number;
  }

  @Override
  public Integer getValue() {
    return number;
  }
}

final class RuleByDay implements Property<ByDay> {
  private ByDay byDay;

  RuleByDay(ByDay byDay) {
    this.byDay = byDay;
  }

  @Override
  public ByDay getValue() {
    return byDay;
  }
}

final class ByDay {
  private boolean isNegative;
  private RuleNumber number;
  private RuleDay day;

  ByDay(boolean isNegative, RuleNumber number, RuleDay day) {
    this.isNegative = isNegative;
    this.number = number;
    this.day = day;
  }
}
