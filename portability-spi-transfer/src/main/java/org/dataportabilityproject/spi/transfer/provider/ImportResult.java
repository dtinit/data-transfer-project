package org.dataportabilityproject.spi.transfer.provider;

/** The result of an item import operation. */
public class ImportResult {
  public static final ImportResult OK = new ImportResult(ResultType.OK);
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
    this.type = type;
    this.message = message;
  }

  /**
   * Ctor used to return error or retry results.
   *
   * @param type the result type
   * @param message the result message, if any
   * @param throwable the exception thrown
   */
  public ImportResult(ResultType type, String message, Throwable throwable) {
    this.type = type;
    this.message = message;
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
