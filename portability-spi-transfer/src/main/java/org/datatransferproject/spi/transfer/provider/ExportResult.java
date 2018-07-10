package org.datatransferproject.spi.transfer.provider;

import com.google.common.base.Preconditions;
import java.util.Optional;
import org.datatransferproject.spi.transfer.types.ContinuationData;
import org.datatransferproject.types.transfer.models.DataModel;

/**
 * The result of an item export operation, after retries.
 */
public class ExportResult<T extends DataModel> {

  public static final ExportResult CONTINUE = new ExportResult(ResultType.CONTINUE);
  public static final ExportResult END = new ExportResult(ResultType.CONTINUE);

  private ResultType type;
  private T exportedData;
  private ContinuationData continuationData;
  // Throwable should be absent unless an error was thrown during export
  private Optional<Throwable> throwable = Optional.empty();

  /**
   * Ctor.
   *
   * @param type the result type
   */
  public ExportResult(ResultType type) {
    verifyNonErrorResultType(type);
    this.type = type;
  }

  /**
   * Ctor.
   *
   * @param type the result type
   * @param exportedData the exported data
   */
  public ExportResult(ResultType type, T exportedData) {
    verifyNonErrorResultType(type);
    this.type = type;
    this.exportedData = exportedData;
  }

  /**
   * Ctor.
   *
   * @param type the result type
   * @param exportedData the exported data
   * @param continuationData continuation information
   */
  public ExportResult(ResultType type, T exportedData, ContinuationData continuationData) {
    verifyNonErrorResultType(type);
    this.type = type;
    this.exportedData = exportedData;
    this.continuationData = continuationData;
  }

  /**
   * Ctor.
   *
   * @param throwable the throwable from execution
   */
  public ExportResult(Throwable throwable) {
    this.type = ResultType.ERROR;
    this.throwable = Optional.of(throwable);
  }

  /**
   * Returns the type of result.
   */
  public ResultType getType() {
    return type;
  }

  /**
   * Returns the exported data.
   */
  public T getExportedData() {
    return exportedData;
  }

  public ContinuationData getContinuationData() {
    return continuationData;
  }

  public Optional<Throwable> getThrowable() {
    return throwable;
  }

  private void verifyNonErrorResultType(ResultType type) {
    String mustHaveThrowable = "ExportResult with ResultType = ERROR must hold a throwable";
    Preconditions.checkArgument(!type.equals(ResultType.ERROR), mustHaveThrowable);
  }

  /**
   * Result types.
   */
  public enum ResultType {
    /**
     * Indicates the operation was successful and more items are available so the export should
     * continue.
     */
    CONTINUE,
    /**
     * Indicates the operation was successful and no more items are available.
     */
    END,
    /**
     * Indicates an unrecoverable error was raised.
     */
    ERROR
  }
}
