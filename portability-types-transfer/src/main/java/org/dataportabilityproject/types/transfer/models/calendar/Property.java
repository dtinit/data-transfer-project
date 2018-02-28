package org.dataportabilityproject.types.transfer.models.calendar;

interface Property<T> {
  T getValue();
}

class RuleDay implements Property<RecurringEvent.Day> {
  private RecurringEvent.Day day;

  RuleDay(RecurringEvent.Day day) {
    this.day = day;
  }

  @Override
  public RecurringEvent.Day getValue() {
    return day;
  }
}

class RuleNumber implements Property<Integer> {
  private Integer number;

  RuleNumber(Integer number) {
    this.number = number;
  }

  @Override
  public Integer getValue() {
    return number;
  }
}

class RuleByDay implements Property<ByDay> {
  private ByDay byDay;

  RuleByDay(ByDay byDay) {
    this.byDay = byDay;
  }

  @Override
  public ByDay getValue() {
    return byDay;
  }
}

class ByDay {
  private boolean isNegative;
  private RuleNumber number;
  private RuleDay day;

  ByDay(boolean isNegative, RuleNumber number, RuleDay day) {
    this.isNegative = isNegative;
    this.number = number;
    this.day = day;
  }
}