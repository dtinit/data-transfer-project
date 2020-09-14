package org.datatransferproject.transfer.koofr;

import com.google.common.collect.ImmutableList;
import java.util.Collection;
import org.datatransferproject.types.common.models.photos.PhotoAlbum;
import org.datatransferproject.types.common.models.photos.PhotoModel;
import org.datatransferproject.types.common.models.photos.PhotosContainerResource;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
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

    Assert.assertEquals(1, albumsArray.length);
    Assert.assertEquals("Album _______________1", albumsArray[0].getName());

    Assert.assertEquals(2, photosArray.length);
    Assert.assertEquals("pic1_______________.jpg", photosArray[0].getTitle());
    Assert.assertEquals("pic2_______________.jpg", photosArray[1].getTitle());
  }

  @Test
  public void testGetAlbumName() {
    KoofrTransmogrificationConfig config = new KoofrTransmogrificationConfig();

    Assert.assertEquals("Valid name", config.getAlbumName("Valid name"));
    Assert.assertEquals("Album", config.getAlbumName(""));
    Assert.assertEquals("Album .", config.getAlbumName("."));
    Assert.assertEquals("Album ..", config.getAlbumName(".."));
  }
}
