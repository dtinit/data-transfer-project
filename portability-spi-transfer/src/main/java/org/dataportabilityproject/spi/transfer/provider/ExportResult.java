package org.dataportabilityproject.spi.transfer.provider;

import com.google.common.base.Preconditions;
import org.dataportabilityproject.spi.transfer.types.ContinuationData;
import org.dataportabilityproject.types.transfer.models.DataModel;

/**
 * The result of an item export operation.
 */
public class ExportResult<T extends DataModel> {

  public static final ExportResult CONTINUE = new ExportResult(ResultType.CONTINUE);
  public static final ExportResult END = new ExportResult(ResultType.CONTINUE);
  private static final String MUST_HAVE_THROWABLE = "ExportResult with ResultType = ERROR must hold a throwable";

  private ResultType type;
  private String message;
  private T exportedData;
  private ContinuationData continuationData;
  private Throwable throwable;

  /**
   * Ctor used to return error or retry results.
   *
   * @param type the result type
   * @param message the result message, if any
   */
  public ExportResult(ResultType type, String message) {
    Preconditions.checkArgument(!type.equals(ResultType.ERROR), MUST_HAVE_THROWABLE);
    this.type = type;
    this.message = message;
  }

  /**
   * Ctor.
   *
   * @param type the result type
   */
  public ExportResult(ResultType type) {
    Preconditions.checkArgument(!type.equals(ResultType.ERROR), MUST_HAVE_THROWABLE);
    this.type = type;
  }

  /**
   * Ctor.
   *
   * @param type the result type
   * @param exportedData the exported data
   */
  public ExportResult(ResultType type, T exportedData) {
    Preconditions.checkArgument(!type.equals(ResultType.ERROR), MUST_HAVE_THROWABLE);
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
    Preconditions.checkArgument(!type.equals(ResultType.ERROR), MUST_HAVE_THROWABLE);
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
    this.throwable = throwable;
  }

  /**
   * Returns the type of result.
   */
  public ResultType getType() {
    return type;
  }

  /**
   * Returns the result message or null if no message is present.
   */
  public String getMessage() {
    return message;
  }

  /**
   * Returns the exported data.
   */
  public T getExportedData() {
    return exportedData;
  }

  public Object getContinuationData() {
    return continuationData;
  }

  public Throwable getThrowable() {
    return throwable;
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
