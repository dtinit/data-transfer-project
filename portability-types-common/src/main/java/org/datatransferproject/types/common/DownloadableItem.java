package org.datatransferproject.types.common;

/**
 * Represent an item we can download through a URL and store in a temporary storage.
 *
 * <p>PhotoModel is a good example. Often, we check if the item is in the job store and download it
 * if it isn't.
 */
public interface DownloadableItem extends ImportableItem {

  /** Remote or local URL used to download and identify an item. */
  String getFetchableUrl();
  boolean isInTempStore();
}
