package org.datatransferproject.spi.transfer.provider;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/** The result of an item import operation, after retries. */
public class ImportResult {

  public static final ImportResult OK = new ImportResult(ResultType.OK);

  private ResultType type;
  // Throwable should be absent unless an error was thrown during export
  private Optional<Throwable> throwable = Optional.empty();
  private Optional<Map<String, Integer>> counts = Optional.empty();
  private Optional<Long> bytes = Optional.empty();

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
   * @param bytes
   */
  public ImportResult(
      ResultType type,
      Optional<Throwable> throwable,
      Optional<Map<String, Integer>> counts,
      Optional<Long> bytes) {
    this.type = type;
    this.throwable = throwable;
    this.counts = counts;
    this.bytes = bytes;
  }

  /** Returns a new result that's a combination of the two given results. */
  public static ImportResult merge(ImportResult ir1, ImportResult ir2) {
    if (ir1.getType() == ResultType.ERROR) {
      return ir1;
    }
    if (ir2.getType() == ResultType.ERROR) {
      return ir2;
    }
    ImportResult res = new ImportResult(ResultType.OK);
    res.bytes = Stream.of(ir1.getBytes(), ir2.getBytes())
        .filter(Optional::isPresent)
        .map(Optional::get)
        .reduce(Long::sum);
    res.counts = mergeCounts(ir1, ir2);
    return res;
  }

  private static Optional<Map<String, Integer>> mergeCounts(ImportResult ir1, ImportResult ir2) {
    if (ir1.counts.isPresent() && ir2.counts.isPresent()) {
      Map<String, Integer> map = new HashMap<>(ir1.counts.get());
      ir2.counts.get().forEach((k, v) -> map.merge(k, v, Integer::sum));
      return Optional.of(map);
    } else {
      return ir1.counts.isPresent() ? ir1.counts : ir2.counts;
    }
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
   * @return An optional which if present is the number of bytes of items transferred. Albums and
   *     other containers are considered to be zero bytes.
   */
  public Optional<Long> getBytes() {
    return bytes;
  }

  /**
   * Creates a shallow copy of the current ImportResult but with the counts field set to the value
   * of the mapping given as an argument
   */
  public ImportResult copyWithCounts(Map<String, Integer> newCounts) {
    return new ImportResult(type, throwable, Optional.ofNullable(newCounts), bytes);
  }

  /**
   * Creates a shallow copy of the current ImportResult but with the bytes field set to the value
   * given as an argument
   */
  public ImportResult copyWithBytes(Long bytes) {
    return new ImportResult(type, throwable, counts, Optional.ofNullable(bytes));
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ImportResult)) {
      return false;
    }
    ImportResult that = (ImportResult) o;
    return type == that.type &&
        Objects.equals(throwable, that.throwable) &&
        Objects.equals(counts, that.counts) &&
        Objects.equals(bytes, that.bytes);
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, throwable, counts, bytes);
  }

  /** Result types. */
  public enum ResultType {
    /** Indicates a successful import. */
    OK,

    /** Indicates an unrecoverable error was raised. */
    ERROR
  }
}
