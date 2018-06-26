package org.dataportabilityproject.spi.transfer.provider;

import com.google.common.base.Preconditions;

/**
 * The result of an item import operation, after retries.
 */
public class ImportResult {

  public static final ImportResult OK = new ImportResult(ResultType.OK);

  private ResultType type;
  private Throwable throwable; // Should be null unless an error was thrown during export

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

  /**
   * Returns the type of result.
   */
  public ResultType getType() {
    return type;
  }

  /**
   * Returns the throwable or null if no throwable is present.
   */
  public Throwable getThrowable() {
    return throwable;
  }

  private void verifyNonErrorResultType(ResultType type) {
    String mustHaveThrowable = "ImportResult with ResultType = ERROR must hold a throwable";
    Preconditions.checkArgument(!type.equals(ResultType.ERROR), mustHaveThrowable);
  }

  /**
   * Result types.
   */
  public enum ResultType {
    /**
     * Indicates a successful import.
     */
    OK,

    /**
     * Indicates an unrecoverable error was raised.
     */
    ERROR
  }
}
