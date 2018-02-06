package org.dataportabilityproject.serviceProviders.flickr;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anySet;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
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
import com.flickr4java.flickr.uploader.UploadMetaData;
import com.flickr4java.flickr.uploader.Uploader;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import org.dataportabilityproject.cloud.interfaces.JobDataCache;
import org.dataportabilityproject.cloud.local.InMemoryJobDataCache;
import org.dataportabilityproject.dataModels.ContinuationInformation;
import org.dataportabilityproject.dataModels.ExportInformation;
import org.dataportabilityproject.dataModels.Resource;
import org.dataportabilityproject.dataModels.photos.PhotoAlbum;
import org.dataportabilityproject.dataModels.photos.PhotoModel;
import org.dataportabilityproject.dataModels.photos.PhotosModelWrapper;
import org.dataportabilityproject.shared.IdOnlyResource;
import org.dataportabilityproject.shared.ImageStreamProvider;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlickrPhotoServiceTest {

  private final Logger logger = LoggerFactory.getLogger(FlickrPhotoServiceTest.class);

  private static final String PHOTO_TITLE = "Title";
  private static final String FETCHABLE_URL = "fetchable_url";
  private static final String PHOTO_DESCRIPTION = "Description";
  private static final String MEDIA_TYPE = "jpeg";

  private static final String ALBUM_ID = "Album ID";
  private static final String ALBUM_NAME = "Album name";
  private static final String ALBUM_DESCRIPTION = "Album description";

  private static final PhotoModel PHOTO_MODEL = new PhotoModel(PHOTO_TITLE, FETCHABLE_URL,
      PHOTO_DESCRIPTION,
      MEDIA_TYPE, ALBUM_ID);
  private static final PhotoAlbum PHOTO_ALBUM = new PhotoAlbum(ALBUM_ID, ALBUM_NAME,
      ALBUM_DESCRIPTION);

  private static final String FLICKR_PHOTO_ID = "flickrPhotoId";
  private static final String FLICKR_ALBUM_ID = "flickrAlbumId";

  private Flickr flickr = mock(Flickr.class);
  private PhotosetsInterface photosetsInterface = mock(PhotosetsInterface.class);
  private PhotosInterface photosInterface = mock(PhotosInterface.class);
  private Uploader uploader = mock(Uploader.class);
  private JobDataCache jobDataCache = new InMemoryJobDataCache();
  private User user = mock(User.class);
  private Auth auth = new Auth(Permission.WRITE, user);
  private ImageStreamProvider imageStreamProvider = mock(ImageStreamProvider.class);
  private BufferedInputStream bufferedInputStream = mock(BufferedInputStream.class);
  private FlickrPhotoService photoService = new FlickrPhotoService(flickr, photosetsInterface,
      photosInterface, uploader, auth, jobDataCache, imageStreamProvider);

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
    Photo photo = initializePhoto(PHOTO_TITLE, FETCHABLE_URL, PHOTO_DESCRIPTION);

    PhotoModel photoModel = FlickrPhotoService.toCommonPhoto(photo, ALBUM_ID);

    assertThat(photoModel.getAlbumId()).isEqualTo(ALBUM_ID);
    assertThat(photoModel.getFetchableUrl()).isEqualTo(FETCHABLE_URL);
    assertThat(photoModel.getTitle()).isEqualTo(PHOTO_TITLE);
    assertThat(photoModel.getDescription()).isEqualTo(PHOTO_DESCRIPTION);
    assertThat(photoModel.getMediaType()).isEqualTo("image/jpeg");
  }

  @Test
  public void importStoresAlbumsInJobCache() throws IOException, FlickrException {
    // Set up input: a single photo album with a single photo
    PhotosModelWrapper wrapper = new PhotosModelWrapper(Collections.singletonList(PHOTO_ALBUM),
        Collections.singletonList(PHOTO_MODEL), new ContinuationInformation(null, null));

    // Set up mocks
    when(imageStreamProvider.get(FETCHABLE_URL)).thenReturn(bufferedInputStream);

    when(uploader
        .upload(any(BufferedInputStream.class), any(UploadMetaData.class)))
        .thenReturn(FLICKR_PHOTO_ID);

    String flickrAlbumTitle = FlickrPhotoService.FLICKR_ALBUM_PREFIX + ALBUM_NAME;
    Photoset photoSet = initializePhotoset(FLICKR_ALBUM_ID, flickrAlbumTitle, ALBUM_DESCRIPTION);
    when(photosetsInterface.create(flickrAlbumTitle, ALBUM_DESCRIPTION, FLICKR_PHOTO_ID))
        .thenReturn(photoSet);

    // Run test
    photoService.importItem(wrapper);

    // Verify the image stream provider got the correct url
    verify(imageStreamProvider).get(FETCHABLE_URL);

    // Verify the correct photo information was uploaded
    ArgumentCaptor<UploadMetaData> uploadMetaDataArgumentCaptor = ArgumentCaptor
        .forClass(UploadMetaData.class);
    verify(uploader)
        .upload(eq(bufferedInputStream), uploadMetaDataArgumentCaptor.capture());
    UploadMetaData actualUploadMetaData = uploadMetaDataArgumentCaptor.getValue();
    assertThat(actualUploadMetaData.getTitle())
        .isEqualTo(FlickrPhotoService.FLICKR_PHOTO_PREFIX + PHOTO_TITLE);
    assertThat(actualUploadMetaData.getDescription()).isEqualTo(PHOTO_DESCRIPTION);

    // Verify the photosets interface got the command to create the correct album
    verify(photosetsInterface).create(flickrAlbumTitle, ALBUM_DESCRIPTION, FLICKR_PHOTO_ID);

    // Check jobDataCache contents
    String expectedAlbumKey = FlickrPhotoService.CACHE_ALBUM_METADATA_PREFIX + ALBUM_ID;
    assertThat(jobDataCache.hasKey(expectedAlbumKey)).isTrue();
    assertThat(jobDataCache.getData(expectedAlbumKey, PhotoAlbum.class)).isEqualTo(PHOTO_ALBUM);
    assertThat(jobDataCache.getData(ALBUM_ID, String.class)).isEqualTo(FLICKR_ALBUM_ID);
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
    photosetList.setPhotosets(Collections.singletonList(photoset));
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

    assertThat(result.getPhotos().size()).isEqualTo(numPhotos);
    assertThat(result.getAlbums()).isEmpty();
    assertThat(result.getContinuationInformation().getSubResources()).isEmpty();
    assertThat(result.getContinuationInformation().getPaginationInformation())
        .isEqualTo(new FlickrPaginationInformation(page + 1));
  }

  private static Photo initializePhoto(String title, String url, String description) {
    Photo photo = new Photo();
    photo.setTitle(title);
    photo.setDescription(description);
    photo.setOriginalFormat(MEDIA_TYPE);
    Size size = new Size();
    size.setSource(url);
    size.setLabel(Size.ORIGINAL);
    photo.setSizes(Collections.singletonList(size));
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
