package org.datatransferproject.types.common.models.calendar;

import java.util.Map;
import javax.annotation.Nullable;
import javax.annotation.processing.Generated;

@Generated("com.google.auto.value.processor.AutoValueProcessor")
final class AutoValue_RecurrenceRule_RRule extends RecurrenceRule.RRule {

  private final RecurrenceRule.Freq freq;

  private final String until;

  private final Integer count;

  private final Integer interval;

  private final Map<RecurrenceRule.ByRule, String> byRuleMap;

  private final RecurrenceRule.Day wkst;

  private AutoValue_RecurrenceRule_RRule(
      RecurrenceRule.Freq freq,
      @Nullable String until,
      @Nullable Integer count,
      @Nullable Integer interval,
      @Nullable Map<RecurrenceRule.ByRule, String> byRuleMap,
      @Nullable RecurrenceRule.Day wkst) {
    this.freq = freq;
    this.until = until;
    this.count = count;
    this.interval = interval;
    this.byRuleMap = byRuleMap;
    this.wkst = wkst;
  }

  @Override
  RecurrenceRule.Freq freq() {
    return freq;
  }

  @Nullable
  @Override
  String until() {
    return until;
  }

  @Nullable
  @Override
  Integer count() {
    return count;
  }

  @Nullable
  @Override
  Integer interval() {
    return interval;
  }

  @Nullable
  @Override
  Map<RecurrenceRule.ByRule, String> byRuleMap() {
    return byRuleMap;
  }

  @Nullable
  @Override
  RecurrenceRule.Day wkst() {
    return wkst;
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof RecurrenceRule.RRule) {
      RecurrenceRule.RRule that = (RecurrenceRule.RRule) o;
      return this.freq.equals(that.freq())
          && (this.until == null ? that.until() == null : this.until.equals(that.until()))
          && (this.count == null ? that.count() == null : this.count.equals(that.count()))
          && (this.interval == null ? that.interval() == null : this.interval.equals(that.interval()))
          && (this.byRuleMap == null ? that.byRuleMap() == null : this.byRuleMap.equals(that.byRuleMap()))
          && (this.wkst == null ? that.wkst() == null : this.wkst.equals(that.wkst()));
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h$ = 1;
    h$ *= 1000003;
    h$ ^= freq.hashCode();
    h$ *= 1000003;
    h$ ^= (until == null) ? 0 : until.hashCode();
    h$ *= 1000003;
    h$ ^= (count == null) ? 0 : count.hashCode();
    h$ *= 1000003;
    h$ ^= (interval == null) ? 0 : interval.hashCode();
    h$ *= 1000003;
    h$ ^= (byRuleMap == null) ? 0 : byRuleMap.hashCode();
    h$ *= 1000003;
    h$ ^= (wkst == null) ? 0 : wkst.hashCode();
    return h$;
  }

  static final class Builder extends RecurrenceRule.RRule.Builder {
    private RecurrenceRule.Freq freq;
    private String until;
    private Integer count;
    private Integer interval;
    private Map<RecurrenceRule.ByRule, String> byRuleMap;
    private RecurrenceRule.Day wkst;
    Builder() {
    }
    @Override
    public RecurrenceRule.RRule.Builder freq(RecurrenceRule.Freq freq) {
      if (freq == null) {
        throw new NullPointerException("Null freq");
      }
      this.freq = freq;
      return this;
    }
    @Override
    public RecurrenceRule.RRule.Builder until(String until) {
      this.until = until;
      return this;
    }
    @Override
    public RecurrenceRule.RRule.Builder count(Integer count) {
      this.count = count;
      return this;
    }
    @Override
    public RecurrenceRule.RRule.Builder interval(Integer interval) {
      this.interval = interval;
      return this;
    }
    @Override
    public RecurrenceRule.RRule.Builder byRuleMap(Map<RecurrenceRule.ByRule, String> byRuleMap) {
      this.byRuleMap = byRuleMap;
      return this;
    }
    @Override
    public RecurrenceRule.RRule.Builder wkst(RecurrenceRule.Day wkst) {
      this.wkst = wkst;
      return this;
    }
    @Override
    public RecurrenceRule.RRule build() {
      if (this.freq == null) {
        String missing = " freq";
        throw new IllegalStateException("Missing required properties:" + missing);
      }
      return new AutoValue_RecurrenceRule_RRule(
          this.freq,
          this.until,
          this.count,
          this.interval,
          this.byRuleMap,
          this.wkst);
    }
  }

}
