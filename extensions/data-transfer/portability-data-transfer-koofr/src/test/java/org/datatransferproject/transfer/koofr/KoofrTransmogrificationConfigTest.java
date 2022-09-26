package org.datatransferproject.transfer.koofr;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.common.collect.ImmutableList;
import java.util.Collection;
import org.datatransferproject.types.common.models.photos.PhotoAlbum;
import org.datatransferproject.types.common.models.photos.PhotoModel;
import org.datatransferproject.types.common.models.photos.PhotosContainerResource;
import org.junit.jupiter.api.Test;
public class KoofrTransmogrificationConfigTest {

  @Test
  public void testPhotoTitleName() {
    Collection<PhotoAlbum> albums =
        ImmutableList.of(new PhotoAlbum("id1", "Album ~\"#%&*:<>?/\\{|}1", "This is a fake album"));

    Collection<PhotoModel> photos =
        ImmutableList.of(
            new PhotoModel(
                "pic1~\"#%&*:<>?/\\{|}.jpg",
                "http://fake.com/1.jpg", "A pic", "image/jpg", "p1", "id1", true),
            new PhotoModel(
                "pic2~\"#%&*:<>?/\\{|}.jpg",
                "https://fake.com/2.png", "fine art", "image/png", "p2", "id1", true));

    PhotosContainerResource container = new PhotosContainerResource(albums, photos);

    KoofrTransmogrificationConfig config = new KoofrTransmogrificationConfig();

    container.transmogrify(config);

    PhotoAlbum[] albumsArray = container.getAlbums().toArray(new PhotoAlbum[0]);
    PhotoModel[] photosArray = container.getPhotos().toArray(new PhotoModel[0]);

    assertEquals(1, albumsArray.length);
    assertEquals("Album _______________1", albumsArray[0].getName());

    assertEquals(2, photosArray.length);
    assertEquals("pic1_______________.jpg", photosArray[0].getTitle());
    assertEquals("pic2_______________.jpg", photosArray[1].getTitle());
  }

  @Test
  public void testGetAlbumName() {
    KoofrTransmogrificationConfig config = new KoofrTransmogrificationConfig();

    assertEquals("Valid name", config.getAlbumName("Valid name"));
    assertEquals("Album", config.getAlbumName(""));
    assertEquals("Album .", config.getAlbumName("."));
    assertEquals("Album ..", config.getAlbumName(".."));
  }
}
