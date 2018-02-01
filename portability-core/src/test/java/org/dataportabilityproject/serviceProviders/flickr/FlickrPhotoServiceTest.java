package org.dataportabilityproject.serviceProviders.flickr;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;

import com.flickr4java.flickr.Flickr;
import com.flickr4java.flickr.FlickrException;
import com.flickr4java.flickr.auth.Auth;
import com.flickr4java.flickr.photos.Photo;
import com.flickr4java.flickr.photos.PhotosInterface;
import com.flickr4java.flickr.photos.Size;
import com.flickr4java.flickr.photosets.Photoset;
import com.flickr4java.flickr.photosets.Photosets;
import com.flickr4java.flickr.photosets.PhotosetsInterface;
import com.flickr4java.flickr.uploader.Uploader;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.dataportabilityproject.cloud.interfaces.JobDataCache;
import org.dataportabilityproject.dataModels.ExportInformation;
import org.dataportabilityproject.dataModels.photos.PhotoAlbum;
import org.dataportabilityproject.dataModels.photos.PhotoModel;
import org.dataportabilityproject.dataModels.photos.PhotosModelWrapper;
import org.junit.Test;
import org.mockito.Mockito;

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

  Flickr flickr = Mockito.mock(Flickr.class);
  PhotosetsInterface photosetsInterface = Mockito.mock(PhotosetsInterface.class);
  PhotosInterface photosInterface = Mockito.mock(PhotosInterface.class);
  Uploader uploader = Mockito.mock(Uploader.class);
  JobDataCache jobDataCache = Mockito.mock(JobDataCache.class);
  Auth auth = Mockito.mock(Auth.class);
  FlickrPhotoService photoService = new FlickrPhotoService(flickr, photosetsInterface,
      photosInterface, uploader, jobDataCache, auth);

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

  @Test
  public void exportAlbumReturnsNextPage() throws IOException, FlickrException {
    ExportInformation emptyExportInfo =
        new ExportInformation(Optional.empty(), Optional.empty());

    // Set up auth
    Mockito.when(auth.getUser().getId()).thenReturn("id");

    // Set up photoset information
    int page = 1;
    Photoset photoset = new Photoset();
    photoset.setId("id");
    photoset.setTitle("title");
    photoset.setDescription("description");
    Photosets photosetList = new Photosets();
    photosetList.setPage(page);
    photosetList.setPages(page + 1);
    photosetList.setPhotosets(Arrays.asList(photoset));
    Mockito.when(photosetsInterface.getList(anyString(), anyInt(), anyInt(), anyString()))
        .thenReturn(new Photosets());

    PhotosModelWrapper result = photoService
        .export(emptyExportInfo);

    assertThat(result.getContinuationInformation().getPaginationInformation())
        .isEqualTo(new FlickrPaginationInformation(page + 1));
  }

  @Test
  public void exportAlbumReturnsPhotoset() {

  }
}
