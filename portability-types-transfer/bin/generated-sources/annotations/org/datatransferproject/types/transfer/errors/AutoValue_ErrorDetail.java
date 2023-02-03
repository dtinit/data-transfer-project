package org.datatransferproject.types.transfer.errors;

import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.processing.Generated;

@Generated("com.google.auto.value.processor.AutoValueProcessor")
final class AutoValue_ErrorDetail extends ErrorDetail {

  private final String id;

  private final String title;

  private final String exception;

  private AutoValue_ErrorDetail(
      String id,
      String title,
      String exception) {
    this.id = id;
    this.title = title;
    this.exception = exception;
  }

  @JsonProperty("id")
  @Override
  public String id() {
    return id;
  }

  @JsonProperty("title")
  @Override
  public String title() {
    return title;
  }

  @JsonProperty("exception")
  @Override
  public String exception() {
    return exception;
  }

  @Override
  public String toString() {
    return "ErrorDetail{"
        + "id=" + id + ", "
        + "title=" + title + ", "
        + "exception=" + exception
        + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof ErrorDetail) {
      ErrorDetail that = (ErrorDetail) o;
      return this.id.equals(that.id())
          && this.title.equals(that.title())
          && this.exception.equals(that.exception());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h$ = 1;
    h$ *= 1000003;
    h$ ^= id.hashCode();
    h$ *= 1000003;
    h$ ^= title.hashCode();
    h$ *= 1000003;
    h$ ^= exception.hashCode();
    return h$;
  }

  static final class Builder extends ErrorDetail.Builder {
    private String id;
    private String title;
    private String exception;
    Builder() {
    }
    @Override
    public ErrorDetail.Builder setId(String id) {
      if (id == null) {
        throw new NullPointerException("Null id");
      }
      this.id = id;
      return this;
    }
    @Override
    public ErrorDetail.Builder setTitle(String title) {
      if (title == null) {
        throw new NullPointerException("Null title");
      }
      this.title = title;
      return this;
    }
    @Override
    public ErrorDetail.Builder setException(String exception) {
      if (exception == null) {
        throw new NullPointerException("Null exception");
      }
      this.exception = exception;
      return this;
    }
    @Override
    public ErrorDetail build() {
      if (this.id == null
          || this.title == null
          || this.exception == null) {
        StringBuilder missing = new StringBuilder();
        if (this.id == null) {
          missing.append(" id");
        }
        if (this.title == null) {
          missing.append(" title");
        }
        if (this.exception == null) {
          missing.append(" exception");
        }
        throw new IllegalStateException("Missing required properties:" + missing);
      }
      return new AutoValue_ErrorDetail(
          this.id,
          this.title,
          this.exception);
    }
  }

}
