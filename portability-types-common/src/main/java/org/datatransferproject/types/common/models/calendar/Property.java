package org.datatransferproject.types.common.models.calendar;

// This interface could be used by other models.  If so, we should move it out and make it more
// broadly useful.
interface Property<T> {
  T getValue();
}

final class RuleDay implements Property<RecurrenceRule.Day> {
  private final RecurrenceRule.Day day;

  RuleDay(RecurrenceRule.Day day) {
    this.day = day;
  }

  @Override
  public RecurrenceRule.Day getValue() {
    return day;
  }
}

final class RuleNumber implements Property<Integer> {
  private final Integer number;

  RuleNumber(Integer number) {
    this.number = number;
  }

  @Override
  public Integer getValue() {
    return number;
  }
}

final class RuleByDay implements Property<ByDay> {
  private final ByDay byDay;

  RuleByDay(ByDay byDay) {
    this.byDay = byDay;
  }

  @Override
  public ByDay getValue() {
    return byDay;
  }
}

final class ByDay {
  private final boolean isNegative;
  private final RuleNumber number;
  private final RuleDay day;

  ByDay(boolean isNegative, RuleNumber number, RuleDay day) {
    this.isNegative = isNegative;
    this.number = number;
    this.day = day;
  }

  boolean getIsNegative() {
    return isNegative;
  }

  RuleNumber getNumber() {
    return number;
  }

  RuleDay getDay() {
    return day;
  }
}
