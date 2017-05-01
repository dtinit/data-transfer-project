package org.dataportabilityproject.dataModels.photos;

import com.google.common.collect.ImmutableList;
import java.util.Collection;
import org.dataportabilityproject.dataModels.ContinuationInformation;
import org.dataportabilityproject.dataModels.DataModel;

/**
 * A Wrapper for all the possible objects that can be returned by a photos exporter.
 */
public class PhotosModelWrapper implements DataModel {
  private final Collection<PhotoAlbum> albums;
  private final Collection<PhotoModel> photos;
  private ContinuationInformation continuationInformation;

  public PhotosModelWrapper(Collection<PhotoAlbum> albums, Collection<PhotoModel> photos,
      ContinuationInformation continuationInformation) {
    this.albums = albums == null ? ImmutableList.of() : albums;
    this.photos = photos == null ? ImmutableList.of() : photos;
    this.continuationInformation = continuationInformation;
  }

  public Collection<PhotoAlbum> getAlbums() {
    return albums;
  }

  public Collection<PhotoModel> getPhotos() {
    return photos;
  }

  @Override
  public ContinuationInformation getContinuationInformation() {
    return continuationInformation;
  }
}
