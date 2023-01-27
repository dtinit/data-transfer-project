package org.datatransferproject.types.common.models.photos;

import com.google.common.truth.Truth;
import java.util.List;
import org.junit.jupiter.api.Test;

public class PhotoAlbumTest {

  private static final String DESCRIPTION = "AlbumDescription";

  @Test
  public void splitSimple() {
    PhotoAlbum originalAlbum = new PhotoAlbum("123", "MyAlbum", DESCRIPTION);
    List<PhotoAlbum> actual = originalAlbum.split(3);
    Truth.assertThat(actual)
        .containsExactly(
            new PhotoAlbum("123-pt1", "123 (1/3)", DESCRIPTION),
            new PhotoAlbum("123-pt2", "123 (2/3)", DESCRIPTION),
            new PhotoAlbum("123-pt3", "123 (3/3)", DESCRIPTION));
  }

  @Test
  public void splitNegative() {
    PhotoAlbum originalAlbum = new PhotoAlbum("123", "MyAlbum", DESCRIPTION);
    List<PhotoAlbum> actual = originalAlbum.split(-1);
    Truth.assertThat(actual).isEmpty();
  }

  @Test
  public void splitSingle() {
    PhotoAlbum originalAlbum = new PhotoAlbum("123", "MyAlbum", DESCRIPTION);
    List<PhotoAlbum> actual = originalAlbum.split(1);
    Truth.assertThat(actual).containsExactly(new PhotoAlbum("123-pt1", "123 (1/1)", DESCRIPTION));
  }

  @Test
  public void cleanNameSimple() {
    PhotoAlbum originalAlbum = new PhotoAlbum("123", "MyAlbum", DESCRIPTION);
    originalAlbum.cleanName("yu", 'X', 6);
    Truth.assertThat(originalAlbum.getName()).isEqualTo("MXAlbX");
  }
}
