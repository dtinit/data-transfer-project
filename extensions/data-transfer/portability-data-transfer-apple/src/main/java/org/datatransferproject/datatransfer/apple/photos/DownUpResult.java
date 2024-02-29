package org.datatransferproject.datatransfer.apple.photos;

import com.google.auto.value.AutoOneOf;

/**
 * {@link AppleMediaInterface}'s internal representation of an download/upload sequence for a single
 * file.
 *
 * <p>Either download or the upload could fail. Or they could both succeed. {@link #isOk} indicates
 * which it is.
 */
@AutoOneOf(DownUpResult.Status.class)
public abstract class DownUpResult {
  public enum Status {
    SUCCESS_DATA_ID,
    ERROR
  }

  public abstract Status getStatus();

  public boolean isOk() {
    return this.getStatus().equals(Status.SUCCESS_DATA_ID);
  }

  /**
   * Data-ID returned from Apple servers, indicaing the upload start is successful.
   *
   * <p>Sometimes called the "single file upload response"
   */
  public abstract String successDataId();

  public abstract Throwable error();

  public static DownUpResult ofDataId(String dataId) {
    return AutoOneOf_DownUpResult.successDataId(dataId);
  }

  public static DownUpResult ofError(Throwable e) {
    return AutoOneOf_DownUpResult.error(e);
  }
}
