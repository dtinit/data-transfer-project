package org.datatransferproject.types.common;

/**
 * Represent an item we that belongs to some user-visible (and possibly user-defined) collection
 * like a folder on a filesystem, an album in a photo album, etc.
 */
public interface FolderItem {
  /** Returns the unique ID of the parent container this item lives in. */
  String getFolderId();
}
