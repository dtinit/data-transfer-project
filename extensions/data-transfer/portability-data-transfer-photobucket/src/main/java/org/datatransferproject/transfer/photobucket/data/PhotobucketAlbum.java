package org.datatransferproject.transfer.photobucket.data;

import org.datatransferproject.types.common.models.DataModel;

public class PhotobucketAlbum extends DataModel {
  private final String photobucketAlbumId;

  public PhotobucketAlbum(String photobucketAlbumId) {
    this.photobucketAlbumId = photobucketAlbumId;
  }

  public String getPbId() {
    return photobucketAlbumId;
  }
}
