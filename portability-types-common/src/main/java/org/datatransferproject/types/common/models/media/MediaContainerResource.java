package org.datatransferproject.types.common.models.media;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import org.datatransferproject.types.common.models.ContainerResource;
import org.datatransferproject.types.common.models.TransmogrificationConfig;
import org.datatransferproject.types.common.models.photos.PhotoModel;
import org.datatransferproject.types.common.models.videos.VideoObject;

@JsonTypeName("MediaContainerResource")
public class MediaContainerResource extends ContainerResource {
  public static final String PHOTOS_COUNT_DATA_NAME = "photosCount";
  public static final String ALBUMS_COUNT_DATA_NAME = "albumsCount";
  public static final String VIDEOS_COUNT_DATA_NAME = "videosCount";
  private static final String ROOT_ALBUM = "Transferred Photos";
  private final Collection<PhotoModel> photos;
  private final Collection<VideoObject> videos;
  private final Collection<MediaAlbum> albums;

  @JsonCreator
  public MediaContainerResource(
      @JsonProperty("albums") Collection<MediaAlbum> albums,
      @JsonProperty("photos") Collection<PhotoModel> photos,
      @JsonProperty("videos") Collection<VideoObject> videos) {
    this.albums = albums == null ? ImmutableList.of() : albums;
    this.photos = photos == null ? ImmutableList.of() : photos;
    this.videos = photos == null ? ImmutableList.of() : videos;
  }

  public Collection<MediaAlbum> getAlbums() {
    return albums;
  }

  public Collection<PhotoModel> getPhotos() {
    return photos;
  }

  public Collection<VideoObject> getVideos() {
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
    transmogrifyAlbums(config);
    transmogrifyTitles(config);
  }

  // Coerce the albums of the transfer using the specification provided, e.g.
  // limiting max album size or grouping un-collected photos into a root album.
  private void transmogrifyAlbums(TransmogrificationConfig config) {
    ensureRootAlbum(config.getAlbumAllowRootPhotos());
    ensureAlbumSize(config.getAlbumMaxSize());
    transmogrifyTitles(config);
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

    for (VideoObject video : videos) {
      video.cleanName(
          config.getPhotoTitleForbiddenCharacters(),
          config.getPhotoTitleReplacementCharacter(),
          config.getPhotoTitleMaxLength());
    }

    for (MediaAlbum album : albums) {
      album.cleanName(
          config.getPhotoTitleForbiddenCharacters(),
          config.getPhotoTitleReplacementCharacter(),
          config.getPhotoTitleMaxLength());
    }
  }

  // Splits albums that are too large into albums that are smaller than {maxSize}.
  // A value of maxSize=-1 signals that there is no maximum
  void ensureAlbumSize(int maxSize) {
    if (maxSize == -1) {
      // No max size; no need to go through that code.
      return;
    }

    // Group photos and videos by albumId
    Multimap<String, PhotoModel> albumGroups = ArrayListMultimap.create();
    for (PhotoModel photo : photos) {
      albumGroups.put(photo.getAlbumId(), photo);
    }

    // Go through groups, splitting up anything that's too big
    for (Entry<String, Collection<PhotoModel>> entry : albumGroups.asMap().entrySet()) {
      if (entry.getValue().size() > maxSize) {
        for (MediaAlbum album : albums) {
          if (album.getId() != entry.getKey()) {
            continue;
          }
          // Create new partial album objects and reassign photos to those albums
          List<MediaAlbum> newAlbums =
              album.split(-Math.floorDiv(-entry.getValue().size(), maxSize));
          Iterator<PhotoModel> remainingPhotos = entry.getValue().iterator();
          for (MediaAlbum newAlbum : newAlbums) {
            for (int i = 0; i < maxSize; i++) {
              remainingPhotos.next().reassignToAlbum(newAlbum.getId());
              if (!remainingPhotos.hasNext()) {
                break;
              }
            }
          }

          // Replace original album in state
          albums.remove(album);
          albums.addAll(newAlbums);
        }
      }
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

    for (VideoObject video : videos) {
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
