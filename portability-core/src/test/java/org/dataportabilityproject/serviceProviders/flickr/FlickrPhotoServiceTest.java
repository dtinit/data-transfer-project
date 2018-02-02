package org.dataportabilityproject.serviceProviders.flickr;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anySet;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.flickr4java.flickr.Flickr;
import com.flickr4java.flickr.FlickrException;
import com.flickr4java.flickr.auth.Auth;
import com.flickr4java.flickr.auth.Permission;
import com.flickr4java.flickr.people.User;
import com.flickr4java.flickr.photos.Photo;
import com.flickr4java.flickr.photos.PhotoList;
import com.flickr4java.flickr.photos.PhotosInterface;
import com.flickr4java.flickr.photos.Size;
import com.flickr4java.flickr.photosets.Photoset;
import com.flickr4java.flickr.photosets.Photosets;
import com.flickr4java.flickr.photosets.PhotosetsInterface;
import com.flickr4java.flickr.uploader.Uploader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import org.dataportabilityproject.cloud.interfaces.JobDataCache;
import org.dataportabilityproject.dataModels.ContinuationInformation;
import org.dataportabilityproject.dataModels.ExportInformation;
import org.dataportabilityproject.dataModels.Resource;
import org.dataportabilityproject.dataModels.photos.PhotoAlbum;
import org.dataportabilityproject.dataModels.photos.PhotoModel;
import org.dataportabilityproject.dataModels.photos.PhotosModelWrapper;
import org.dataportabilityproject.shared.IdOnlyResource;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlickrPhotoServiceTest {

  private final Logger logger = LoggerFactory.getLogger(FlickrPhotoServiceTest.class);

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

  Flickr flickr = mock(Flickr.class);
  PhotosetsInterface photosetsInterface = mock(PhotosetsInterface.class);
  PhotosInterface photosInterface = mock(PhotosInterface.class);
  Uploader uploader = mock(Uploader.class);
  JobDataCache jobDataCache = mock(JobDataCache.class);
  User user = mock(User.class);
  Auth auth = new Auth(Permission.WRITE, user);
  FlickrPhotoService photoService = new FlickrPhotoService(flickr, photosetsInterface,
      photosInterface, uploader, auth, jobDataCache);

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
    Photo photo = initializePhoto(PHOTO_TITLE, FETCHABLE_URL, DESCRIPTION);

    PhotoModel photoModel = FlickrPhotoService.toCommonPhoto(photo, ALBUM_ID);

    assertThat(photoModel.getAlbumId()).isEqualTo(ALBUM_ID);
    assertThat(photoModel.getFetchableUrl()).isEqualTo(FETCHABLE_URL);
    assertThat(photoModel.getTitle()).isEqualTo(PHOTO_TITLE);
    assertThat(photoModel.getDescription()).isEqualTo(DESCRIPTION);
    assertThat(photoModel.getMediaType()).isEqualTo("image/jpeg");
  }

  @Test
  public void exportAlbumInitial() throws IOException, FlickrException {
    // Set up initial export information, such as what FlickrPhotoService would see when a transfer
    // is initiated
    ExportInformation emptyExportInfo =
        new ExportInformation(Optional.empty(), Optional.empty());

    // Set up auth
    when(user.getId()).thenReturn("userId");

    // Set up photoset
    String photosetId = "photosetId";
    String photosetTitle = "title";
    String photosetDescription = "description";
    Photoset photoset = initializePhotoset(photosetId, photosetTitle, photosetDescription);

    // Set up photosets list (aka album view)
    int page = 1;
    Photosets photosetList = new Photosets();
    photosetList.setPage(page);
    photosetList.setPages(page + 1);
    photosetList.setPhotosets(Arrays.asList(photoset));
    when(photosetsInterface.getList(anyString(), anyInt(), anyInt(), anyString()))
        .thenReturn(photosetList);

    // Run test
    PhotosModelWrapper result = photoService.export(emptyExportInfo);

    // Make sure album/photo information is correct
    assertThat(result.getPhotos()).isEmpty();
    Collection<PhotoAlbum> albums = result.getAlbums();
    assertThat(albums.size()).isEqualTo(1);
    assertThat(albums)
        .containsExactly(new PhotoAlbum(photosetId, photosetTitle, photosetDescription));

    // Make sure continuation information is correct
    ContinuationInformation continuationInformation = result.getContinuationInformation();

    assertThat((FlickrPaginationInformation) continuationInformation.getPaginationInformation())
        .isEqualTo(new FlickrPaginationInformation(page + 1));

    Collection<? extends Resource> subResources = continuationInformation.getSubResources();
    assertThat(subResources.size()).isEqualTo(1);
    assertThat(subResources).containsExactly(new IdOnlyResource(photosetId));
  }

  @Test
  public void exportNextAlbum() {
    // TODO(olsona)
  }

  @Test
  public void exportPhotosFromPhotoset() throws FlickrException, IOException {
    // Situation: getting photos from a set with id photosetsId and page 1
    int page = 1;
    String photosetsId = "photosetsId";
    ExportInformation exportInformation = new ExportInformation(
        Optional.of(new IdOnlyResource(photosetsId)), Optional.empty());

    // Make a bunch of photos, add them to PhotoList, and add pagination information
    int numPhotos = 4;
    PhotoList<Photo> listOfPhotos = new PhotoList<>();
    for (int i = 0; i < numPhotos; i++) {
      Photo photo = initializePhoto("title" + i, "url" + i, "description" + i);
      listOfPhotos.add(photo);
    }
    listOfPhotos.setPage(page);
    listOfPhotos.setPages(page + 1);

    when(photosetsInterface.getPhotos(anyString(), anySet(), anyInt(), anyInt(), anyInt()))
        .thenReturn(listOfPhotos);

    // Run test
    PhotosModelWrapper result = photoService.export(exportInformation);
    logger.debug("Result: {}", result);

    assertThat(result.getPhotos().size()).isEqualTo(numPhotos);
    assertThat(result.getAlbums()).isEmpty();
    assertThat(result.getContinuationInformation().getSubResources()).isEmpty();
    assertThat(result.getContinuationInformation().getPaginationInformation())
        .isEqualTo(new FlickrPaginationInformation(page + 1));
  }

  @Test
  public void exportNextPhotosFromAlbum() {
    // TODO(olsona)
  }

  private static Photo initializePhoto(String title, String url, String description) {
    Photo photo = new Photo();
    photo.setTitle(title);
    photo.setDescription(description);
    photo.setOriginalFormat(MEDIA_TYPE);
    Size size = new Size();
    size.setSource(url);
    size.setLabel(Size.ORIGINAL);
    photo.setSizes(Arrays.asList(size));
    return photo;
  }

  private static Photoset initializePhotoset(String id, String title, String description) {
    Photoset photoset = new Photoset();
    photoset.setId(id);
    photoset.setTitle(title);
    photoset.setDescription(description);
    return photoset;
  }
}
