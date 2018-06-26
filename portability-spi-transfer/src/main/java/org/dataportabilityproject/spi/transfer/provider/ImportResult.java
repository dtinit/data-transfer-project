package org.dataportabilityproject.spi.transfer.provider;

import com.google.common.base.Preconditions;

/** The result of an item import operation. */
public class ImportResult {
  public static final ImportResult OK = new ImportResult(ResultType.OK);
  private static final String MUST_HAVE_THROWABLE = "ImportResult with ResultType = ERROR must hold a throwable";

  private ResultType type;
  private String message;
  private Throwable throwable;

  /**
   * Ctor used to return error or retry results.
   *
   * @param type the result type
   * @param message the result message, if any
   */
  public ImportResult(ResultType type, String message) {
    Preconditions.checkArgument(!type.equals(ResultType.ERROR), MUST_HAVE_THROWABLE);
    this.type = type;
    this.message = message;
  }

  /**
   * Ctor used to return error or retry results.
   *
   * @param throwable the exception thrown
   */
  public ImportResult(Throwable throwable) {
    this.type = ResultType.ERROR;
    this.throwable = throwable;
  }

  /**
   * Ctor.
   *
   * @param type the result type
   */
  public ImportResult(ResultType type) {
    this.type = type;
  }

  /** Returns the type of result. */
  public ResultType getType() {
    return type;
  }

  /** Returns the result message or null if no message is present. */
  public String getMessage() {
    return message;
  }

  /** Returns the throwable or null if no throwable is present. */
  public Throwable getThrowable() { return throwable; }

  /** Result types. */
  public enum ResultType {
    /** Indicates a successful import. */
    OK,

    /** Indicates an unrecoverable error was raised. */
    ERROR
  }
}
