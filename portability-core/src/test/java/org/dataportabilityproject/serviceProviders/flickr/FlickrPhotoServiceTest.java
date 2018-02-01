package org.dataportabilityproject.serviceProviders.flickr;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.flickr4java.flickr.photos.Photo;
import com.flickr4java.flickr.photos.Size;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.dataportabilityproject.dataModels.photos.PhotoAlbum;
import org.dataportabilityproject.dataModels.photos.PhotoModel;
import org.junit.Test;

public class FlickrPhotoServiceTest {

  static final String PHOTO_TITLE = "Title";
  static final String FETCHABLE_URL = "fetchable_url";
  static final String DESCRIPTION = "Description";
  static final String MEDIA_TYPE = "jpeg";

  static final String ALBUM_ID = "Album ID";
  static final String ALBUM_NAME = "Album name";
  static final String ALBUM_DESCRIPTION = "Album description";

  static final PhotoModel PHOTO_MODEL = new PhotoModel(PHOTO_TITLE, FETCHABLE_URL, DESCRIPTION,
      MEDIA_TYPE, ALBUM_ID);
  static final PhotoAlbum PHOTO_ALBUM = new PhotoAlbum(ALBUM_ID, ALBUM_NAME, ALBUM_DESCRIPTION);

  @Test
  public void getPage() {
    // No pagination information, so we must be on page 1
    assertThat(FlickrPhotoService.getPage(Optional.empty())).isEqualTo(1);

    // Correctly get current page
    int page = 6;
    FlickrPaginationInformation paginationInformation = new FlickrPaginationInformation(page);
    assertThat(FlickrPhotoService.getPage(Optional.of(paginationInformation))).isEqualTo(page);
  }

  @Test
  public void getMimeType() {
    assertThat(FlickrPhotoService.toMimeType("jpeg")).isEqualTo("image/jpeg");
    assertThrows(IllegalArgumentException.class, () -> FlickrPhotoService.toMimeType("png"));
  }

  @Test
  public void toCommonPhoto() {
    Photo photo = new Photo();
    photo.setTitle(PHOTO_TITLE);
    Size size = new Size();
    size.setSource(FETCHABLE_URL);
    size.setLabel(Size.ORIGINAL);
    List<Size> sizeList = Arrays.asList(size);
    photo.setSizes(sizeList);
    photo.setDescription(DESCRIPTION);
    photo.setOriginalFormat(MEDIA_TYPE);

    PhotoModel photoModel = FlickrPhotoService.toCommonPhoto(photo, ALBUM_ID);
    assertThat(photoModel.getAlbumId()).isEqualTo(ALBUM_ID);
    assertThat(photoModel.getFetchableUrl()).isEqualTo(FETCHABLE_URL);
    assertThat(photoModel.getTitle()).isEqualTo(PHOTO_TITLE);
    assertThat(photoModel.getDescription()).isEqualTo(DESCRIPTION);
    assertThat(photoModel.getMediaType()).isEqualTo("image/jpeg");
  }
}
