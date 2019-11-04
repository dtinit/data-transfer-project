package org.datatransferproject.spi.transfer.provider;

import java.util.Map;
import java.util.Optional;

/** The result of an item import operation, after retries. */
public class ImportResult {

  public static final ImportResult OK = new ImportResult(ResultType.OK);

  private ResultType type;
  // Throwable should be absent unless an error was thrown during export
  private Optional<Throwable> throwable = Optional.empty();
  private Optional<Map<String, Integer>> counts = Optional.empty();

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
   * Ctor.
   *
   * @param type the result type
   * @param throwable the exception thrown
   * @param counts mapping representing the number of imported items
   */
  public ImportResult(ResultType type, Optional<Throwable> throwable, Map<String, Integer> counts) {
    this.type = type;
    this.throwable = throwable;
    this.counts = Optional.ofNullable(counts);
  }

  /** Returns the type of result. */
  public ResultType getType() {
    return type;
  }

  /** Returns the throwable or an empty optional if no throwable is present. */
  public Optional<Throwable> getThrowable() {
    return throwable;
  }

  /** Returns the number of imported items or an empty optional if no mapping is present. */
  public Optional<Map<String, Integer>> getCounts() {
    return counts;
  }

  /**
   * Creates a shallow copy of the current ImportResult but with the counts field set to the value
   * of the mapping given as an argument
   */
  public ImportResult copyWithCounts(Map<String, Integer> newCounts) {
    return new ImportResult(type, throwable, newCounts);
  }

  /** Result types. */
  public enum ResultType {
    /** Indicates a successful import. */
    OK,

    /** Indicates an unrecoverable error was raised. */
    ERROR
  }
}
