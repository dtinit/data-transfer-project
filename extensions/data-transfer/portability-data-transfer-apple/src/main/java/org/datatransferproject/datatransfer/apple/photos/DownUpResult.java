package org.datatransferproject.datatransfer.apple.photos;

import com.google.auto.value.AutoOneOf;

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
