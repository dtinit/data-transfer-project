/*
 * Copyright 2018 The Data Transfer Project Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.datatransferproject.types.common.models.photos;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import org.datatransferproject.types.common.models.ContainerResource;
import org.datatransferproject.types.common.models.TransmogrificationConfig;

/** A Wrapper for all the possible objects that can be returned by a photos exporter. */
@JsonTypeName("PhotosContainerResource")
public class PhotosContainerResource extends ContainerResource {

  public static final String PHOTOS_COUNT_DATA_NAME = "photosCount";
  public static final String ALBUMS_COUNT_DATA_NAME = "albumsCount";
  private static final String ROOT_ALBUM = "Transferred Photos";

  private Collection<PhotoAlbum> albums;
  private final Collection<PhotoModel> photos;

  @JsonCreator
  public PhotosContainerResource(
          @JsonProperty("albums") Collection<PhotoAlbum> albums,
          @JsonProperty("photos") Collection<PhotoModel> photos) {
    this.albums = albums == null ? ImmutableList.of() : albums;
    this.photos = photos == null ? ImmutableList.of() : photos;
  }

  public Collection<PhotoAlbum> getAlbums() {
    return albums;
  }

  public Collection<PhotoModel> getPhotos() {
    return photos;
  }

  @Override
  public Map<String, Integer> getCounts() {
    return new ImmutableMap.Builder<String, Integer>()
            .put(PHOTOS_COUNT_DATA_NAME, photos.size())
            .put(ALBUMS_COUNT_DATA_NAME, albums.size())
            .build();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    PhotosContainerResource that = (PhotosContainerResource) o;
    return Objects.equals(getAlbums(), that.getAlbums()) &&
            Objects.equals(getPhotos(), that.getPhotos());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getAlbums(), getPhotos());
  }

  public void transmogrify(TransmogrificationConfig config) {
    transmogrifyAlbums(config);
    transmogrifyPhotos(config);
  }

  // Coerce the albums of the transfer using the specification provided, e.g.
  // limiting max album size or grouping un-collected photos into a root album.
  private void transmogrifyAlbums(TransmogrificationConfig config) {
    ensureRootAlbum(config.getAlbumAllowRootPhotos());
    ensureAlbumSize(config.getAlbumMaxSize());
    ensureCleanAlbumNames(config.getAlbumNameForbiddenCharacters(),
            config.getAlbumNameReplacementCharacter(),
            config.getAlbumNameMaxLength());
  }

  // Splits albumns that are too large into albums that are smaller than {maxSize}.
  // A value of maxSize=-1 signals that there is no maximum
  void ensureAlbumSize(int maxSize){
    if (maxSize == -1){
      // No max size; no need to go through that code.
      return;
    }
    // Group photos by albumId
    Multimap<String, PhotoModel> albumGroups = ArrayListMultimap.create();
    for (PhotoModel photo: photos){
      albumGroups.put(photo.getAlbumId(), photo);
    }
    // Go through groups, splitting up anything that's too big
    for(Entry<String,Collection<PhotoModel>> entry : albumGroups.asMap().entrySet()){
      if (entry.getValue().size() > maxSize) {
        for(PhotoAlbum album : albums){
          if (album.getId() != entry.getKey()){
            continue;
          }
          // Create new partial album objects and reassign photos to those albums
          List<PhotoAlbum> newAlbums = album.split(-Math.floorDiv(- entry.getValue().size(), maxSize));
          Iterator<PhotoModel> remainingPhotos = entry.getValue().iterator();
          for (PhotoAlbum newAlbum: newAlbums){
            for (int i = 0; i < maxSize; i++){
              remainingPhotos.next().reassignToAlbum(newAlbum.getId());
              if (!remainingPhotos.hasNext()){
                break;
              }
            }
          }

          // Replace original album in state
          List<PhotoAlbum> albums_ = new ArrayList<PhotoAlbum>(albums);
          albums_.remove(album);
          albums_.addAll(newAlbums);
          this.albums = albums_;
        }
      }
    }
  }

  // Ensures that the model obeys the restrictions of the destination service, grouping all
  // un-nested photos into their own root album if allowRootPhotos is true, noop otherwise
  void ensureRootAlbum(boolean allowRootPhotos){
    if (allowRootPhotos) {
      return;
    }
    PhotoAlbum rootAlbum = new PhotoAlbum(
            ROOT_ALBUM,
            ROOT_ALBUM,
            "A copy of your transferred photos that were not in any album"
    );
    boolean usedRootAlbum = false;

    for (PhotoModel photo: photos){
      if (photo.getAlbumId() == null) {
        photo.reassignToAlbum(rootAlbum.getId());
        usedRootAlbum = true;
      }
    }
    if (usedRootAlbum){
      List<PhotoAlbum> albums_ = new ArrayList<PhotoAlbum>(albums);
      albums_.add(rootAlbum);
      this.albums = albums_;
    }
  }

  // Replaces forbidden characters and makes sure that the name is not too long
  void ensureCleanAlbumNames(String forbiddenTitleCharacters, char replacementCharacter, int maxTitleLength) {
    for (PhotoAlbum album: albums) {
      album.cleanName(forbiddenTitleCharacters, replacementCharacter, maxTitleLength);
    }
  }

  // Coerce the photos of the transfer using the specification provided, e.g.
  // limiting max title length or removing forbidden characters, etc.
  private void transmogrifyPhotos(TransmogrificationConfig config) {
    ensureCleanPhotoTitles(
            config.getPhotoTitleForbiddenCharacters(),
            config.getPhotoTitleReplacementCharater(),
            config.getPhotoTitleMaxLength());
  }

  // Replaces forbidden characters and makes sure that the title is not too long
  void ensureCleanPhotoTitles(String forbiddenTitleCharacters, char replacementCharacter, int maxTitleLength) {
    for (PhotoModel photo: photos) {
      photo.cleanTitle(forbiddenTitleCharacters, replacementCharacter, maxTitleLength);
    }
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
            .add("albums", getAlbums())
            .add("photos", getPhotos())
            .add("counts", getCounts())
            .toString();
  }
}
