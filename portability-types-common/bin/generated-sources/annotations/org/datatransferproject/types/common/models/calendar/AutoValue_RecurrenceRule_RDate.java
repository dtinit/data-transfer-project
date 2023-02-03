package org.datatransferproject.types.common.models.calendar;

import java.util.List;
import javax.annotation.Nullable;
import javax.annotation.processing.Generated;

@Generated("com.google.auto.value.processor.AutoValueProcessor")
final class AutoValue_RecurrenceRule_RDate extends RecurrenceRule.RDate {

  private final RecurrenceRule.TimeType value;

  private final String tzidparam;

  private final List<String> rdtval;

  private AutoValue_RecurrenceRule_RDate(
      @Nullable RecurrenceRule.TimeType value,
      @Nullable String tzidparam,
      List<String> rdtval) {
    this.value = value;
    this.tzidparam = tzidparam;
    this.rdtval = rdtval;
  }

  @Nullable
  @Override
  RecurrenceRule.TimeType value() {
    return value;
  }

  @Nullable
  @Override
  String tzidparam() {
    return tzidparam;
  }

  @Override
  List<String> rdtval() {
    return rdtval;
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof RecurrenceRule.RDate) {
      RecurrenceRule.RDate that = (RecurrenceRule.RDate) o;
      return (this.value == null ? that.value() == null : this.value.equals(that.value()))
          && (this.tzidparam == null ? that.tzidparam() == null : this.tzidparam.equals(that.tzidparam()))
          && this.rdtval.equals(that.rdtval());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h$ = 1;
    h$ *= 1000003;
    h$ ^= (value == null) ? 0 : value.hashCode();
    h$ *= 1000003;
    h$ ^= (tzidparam == null) ? 0 : tzidparam.hashCode();
    h$ *= 1000003;
    h$ ^= rdtval.hashCode();
    return h$;
  }

  static final class Builder extends RecurrenceRule.RDate.Builder {
    private RecurrenceRule.TimeType value;
    private String tzidparam;
    private List<String> rdtval;
    Builder() {
    }
    @Override
    RecurrenceRule.RDate.Builder value(RecurrenceRule.TimeType value) {
      this.value = value;
      return this;
    }
    @Override
    RecurrenceRule.RDate.Builder tzidparam(String tzidparam) {
      this.tzidparam = tzidparam;
      return this;
    }
    @Override
    RecurrenceRule.RDate.Builder rdtval(List<String> rdtval) {
      if (rdtval == null) {
        throw new NullPointerException("Null rdtval");
      }
      this.rdtval = rdtval;
      return this;
    }
    @Override
    RecurrenceRule.RDate build() {
      if (this.rdtval == null) {
        String missing = " rdtval";
        throw new IllegalStateException("Missing required properties:" + missing);
      }
      return new AutoValue_RecurrenceRule_RDate(
          this.value,
          this.tzidparam,
          this.rdtval);
    }
  }

}
