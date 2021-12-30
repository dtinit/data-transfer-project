package org.datatransferproject.types.common.models.media;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.datatransferproject.types.common.models.photos.PhotoAlbum;

public class MediaAlbum {
  private final String id;
  private String name;
  private final String description;

  /** The {@code id} is used to associate photos with this album. * */
  @JsonCreator
  public MediaAlbum(
      @JsonProperty("id") String id,
      @JsonProperty("name") String name,
      @JsonProperty("description") String description) {
    Preconditions.checkNotNull(id);
    this.id = id;
    this.name = name;
    this.description = description;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public String getId() {
    return id;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("id", getId())
        .add("name", getName())
        .add("description", getDescription())
        .toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    MediaAlbum that = (MediaAlbum) o;
    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  // This allows us to make album names palatable, removing unpalatable characters and
  // enforcing length rules
  public void cleanName(String forbiddenCharacters, char replacementCharacter, int maxLength) {
    // An album name is allowed to be null, handled on the importer level if there is a problem with
    // this value, so we support it here
    if (name == null) {
      return;
    }
    name = name.chars()
        .mapToObj(c -> (char) c)
        .map(c -> forbiddenCharacters.contains(Character.toString(c)) ? replacementCharacter : c)
        .map(Object::toString)
        .collect(Collectors.joining("")).trim();
    if (maxLength <= 0 || maxLength >= name.length()) {
      return;
    }
    name = name.substring(0, maxLength).trim();
  }
}
