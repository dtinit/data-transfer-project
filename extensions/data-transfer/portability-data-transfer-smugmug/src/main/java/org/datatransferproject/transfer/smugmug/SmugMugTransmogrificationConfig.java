package org.datatransferproject.transfer.smugmug;

import org.datatransferproject.types.common.models.TransmogrificationConfig;

// This class defines transmogrification paramaters for SmugMug imports
public class SmugMugTransmogrificationConfig extends TransmogrificationConfig {
  // Smugmug doesn't allow photos to exist outside of a folder
  private static final boolean ALBUM_ALLOW_ROOT_PHOTOS = false;

  // Album size specified here:
  // https://news.smugmug.com/uploading-to-smugmug-what-how-big-and-how-many-d9df14b07bda
  private static final int ALBUM_MAX_SIZE = 5000;

  /** We need to override the methods to return the updated limits */
  @Override
  public boolean getAlbumAllowRootPhotos() {
    return ALBUM_ALLOW_ROOT_PHOTOS;
  }

  @Override
  public int getAlbumMaxSize() {
    return ALBUM_MAX_SIZE;
  }
}
