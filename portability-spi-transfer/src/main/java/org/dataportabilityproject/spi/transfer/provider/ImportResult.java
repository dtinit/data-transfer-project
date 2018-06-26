package org.dataportabilityproject.spi.transfer.provider;

import com.google.common.base.Preconditions;
import java.util.Optional;

/**
 * The result of an item import operation, after retries.
 */
public class ImportResult {

  public static final ImportResult OK = new ImportResult(ResultType.OK);

  private ResultType type;
  // Throwable should be absent unless an error was thrown during export
  private Optional<Throwable> throwable = Optional.empty();

  /**
   * Ctor used to return error or retry results.
   *
   * @param throwable the exception thrown
   */
  public ImportResult(Throwable throwable) {
    this.type = ResultType.ERROR;
    this.throwable = Optional.of(throwable);
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
  public Optional<Throwable> getThrowable() {
    return throwable;
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
