package org.datatransferproject.types.common.models.calendar;

import java.util.List;
import javax.annotation.Nullable;
import javax.annotation.processing.Generated;

@Generated("com.google.auto.value.processor.AutoValueProcessor")
final class AutoValue_RecurrenceRule_ExDate extends RecurrenceRule.ExDate {

  private final RecurrenceRule.TimeType value;

  private final String tzidparam;

  private final List<String> exdtval;

  private AutoValue_RecurrenceRule_ExDate(
      @Nullable RecurrenceRule.TimeType value,
      @Nullable String tzidparam,
      List<String> exdtval) {
    this.value = value;
    this.tzidparam = tzidparam;
    this.exdtval = exdtval;
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
  List<String> exdtval() {
    return exdtval;
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof RecurrenceRule.ExDate) {
      RecurrenceRule.ExDate that = (RecurrenceRule.ExDate) o;
      return (this.value == null ? that.value() == null : this.value.equals(that.value()))
          && (this.tzidparam == null ? that.tzidparam() == null : this.tzidparam.equals(that.tzidparam()))
          && this.exdtval.equals(that.exdtval());
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
    h$ ^= exdtval.hashCode();
    return h$;
  }

  static final class Builder extends RecurrenceRule.ExDate.Builder {
    private RecurrenceRule.TimeType value;
    private String tzidparam;
    private List<String> exdtval;
    Builder() {
    }
    @Override
    public RecurrenceRule.ExDate.Builder value(RecurrenceRule.TimeType value) {
      this.value = value;
      return this;
    }
    @Override
    public RecurrenceRule.ExDate.Builder tzidparam(String tzidparam) {
      this.tzidparam = tzidparam;
      return this;
    }
    @Override
    public RecurrenceRule.ExDate.Builder exdtval(List<String> exdtval) {
      if (exdtval == null) {
        throw new NullPointerException("Null exdtval");
      }
      this.exdtval = exdtval;
      return this;
    }
    @Override
    public RecurrenceRule.ExDate build() {
      if (this.exdtval == null) {
        String missing = " exdtval";
        throw new IllegalStateException("Missing required properties:" + missing);
      }
      return new AutoValue_RecurrenceRule_ExDate(
          this.value,
          this.tzidparam,
          this.exdtval);
    }
  }

}
