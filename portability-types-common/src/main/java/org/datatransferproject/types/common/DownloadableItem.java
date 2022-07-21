package org.datatransferproject.types.common;

/**
 * Represent an item we can download through a URL and store in a temporary storage. PhotoModel is a
 * good example. Often, we check if the item is in the job store and download it if it isn't.
 */
public interface DownloadableItem extends ImportableItem {

  String getFetchableUrl();

  boolean isInTempStore();
}
