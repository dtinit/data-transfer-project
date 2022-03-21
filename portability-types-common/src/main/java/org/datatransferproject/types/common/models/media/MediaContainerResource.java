package org.datatransferproject.types.common.models.media;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import org.datatransferproject.types.common.models.ContainerResource;
import org.datatransferproject.types.common.models.TransmogrificationConfig;
import org.datatransferproject.types.common.models.photos.PhotoModel;
import org.datatransferproject.types.common.models.videos.VideoModel;

@JsonTypeName("MediaContainerResource")
public class MediaContainerResource extends ContainerResource {
  public static final String PHOTOS_COUNT_DATA_NAME = "photosCount";
  public static final String ALBUMS_COUNT_DATA_NAME = "albumsCount";
  public static final String VIDEOS_COUNT_DATA_NAME = "videosCount";
  private static final String ROOT_ALBUM = "Transferred Photos";
  private final Collection<PhotoModel> photos;
  private final Collection<VideoModel> videos;
  private final Collection<MediaAlbum> albums;

  @JsonCreator
  public MediaContainerResource(
      @JsonProperty("albums") Collection<MediaAlbum> albums,
      @JsonProperty("photos") Collection<PhotoModel> photos,
      @JsonProperty("videos") Collection<VideoModel> videos) {
    this.albums = albums == null ? ImmutableList.of() : albums;
    this.photos = photos == null ? ImmutableList.of() : photos;
    this.videos = videos == null ? ImmutableList.of() : videos;
  }

  public Collection<MediaAlbum> getAlbums() {
    return albums;
  }

  public Collection<PhotoModel> getPhotos() {
    return photos;
  }

  public Collection<VideoModel> getVideos() {
    return videos;
  }

  @Override
  public Map<String, Integer> getCounts() {
    return new ImmutableMap.Builder<String, Integer>()
        .put(ALBUMS_COUNT_DATA_NAME, albums.size())
        .put(PHOTOS_COUNT_DATA_NAME, photos.size())
        .put(VIDEOS_COUNT_DATA_NAME, videos.size())
        .build();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    MediaContainerResource that = (MediaContainerResource) o;
    return Objects.equals(getAlbums(), that.getAlbums())
        && Objects.equals(getPhotos(), that.getPhotos())
        && Objects.equals(getVideos(), that.getVideos());
  }

  public void transmogrify(TransmogrificationConfig config) {
    ensureRootAlbum(config.getAlbumAllowRootPhotos());
    transmogrifyTitles(config);

    // TODO(#1000): This splitting code isn't entirely correct since it assumes all the album items
    //  are present in this resource, when they could be split up it also assumes that all the
    //  albums that the images correspond to are also in this resource which may not be the case
    //  so in the meantime, i'm removing this code and it can be added in in the future.
    //  ensureAlbumSize(config.getAlbumMaxSize());
  }

  // Coerce the photos of the transfer using the specification provided, e.g.
  // limiting max title length or removing forbidden characters, etc.
  private void transmogrifyTitles(TransmogrificationConfig config) {
    // Replaces forbidden characters and makes sure that the title is not too long
    for (PhotoModel photo : photos) {
      photo.cleanTitle(
          config.getPhotoTitleForbiddenCharacters(),
          config.getPhotoTitleReplacementCharacter(),
          config.getPhotoTitleMaxLength());
    }

    for (VideoModel video : videos) {
      video.cleanName(
          config.getVideoTitleForbiddenCharacters(),
          config.getVideoTitleReplacementCharacter(),
          config.getVideoTitleMaxLength());
    }

    for (MediaAlbum album : albums) {
      album.cleanName(
          config.getAlbumNameForbiddenCharacters(),
          config.getAlbumNameReplacementCharacter(),
          config.getAlbumNameMaxLength());
    }
  }

  // Ensures that the model obeys the restrictions of the destination service, grouping all
  // un-nested photos into their own root album if allowRootPhotos is true, noop otherwise
  void ensureRootAlbum(boolean allowRootPhotos) {
    if (allowRootPhotos) {
      return;
    }
    MediaAlbum rootAlbum =
        new MediaAlbum(
            ROOT_ALBUM, ROOT_ALBUM, "A copy of your transferred media that were not in any album");
    boolean usedRootAlbum = false;

    for (PhotoModel photo : photos) {
      if (photo.getAlbumId() == null) {
        photo.reassignToAlbum(rootAlbum.getId());
        usedRootAlbum = true;
      }
    }

    for (VideoModel video : videos) {
      if (video.getAlbumId() == null) {
        video.reassignToAlbum(rootAlbum.getId());
        usedRootAlbum = true;
      }
    }

    if (usedRootAlbum) {
      albums.add(rootAlbum);
    }
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("albums", getAlbums())
        .add("photos", getPhotos())
        .add("videos", getVideos())
        .add("counts", getCounts())
        .toString();
  }

  @Override
  public int hashCode() {
    return Objects.hash(getAlbums(), getPhotos(), getVideos());
  }
}
