package org.datatransferproject.types.common.models.media;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.truth.Truth;
import java.util.List;
import java.util.stream.Collectors;
import org.datatransferproject.types.common.models.ContainerResource;
import org.datatransferproject.types.common.models.TransmogrificationConfig;
import org.datatransferproject.types.common.models.photos.PhotoAlbum;
import org.datatransferproject.types.common.models.photos.PhotoModel;
import org.datatransferproject.types.common.models.photos.PhotosContainerResource;
import org.datatransferproject.types.common.models.videos.VideoAlbum;
import org.datatransferproject.types.common.models.videos.VideoModel;
import org.datatransferproject.types.common.models.videos.VideosContainerResource;
import org.junit.jupiter.api.Test;

// TODO(#1060) this code was ported over without unit tests; below is a mostly 1:1-port
// backfill but the new video handling logic in MediaContainerResource should have test coverage
// too.
public class MediaContainerResourceTest {

  @Test
  public void verifySerializeDeserialize() throws Exception {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerSubtypes(MediaContainerResource.class);

    List<MediaAlbum> albums =
        ImmutableList.of(new MediaAlbum("id1", "albumb1", "This is a fake albumb"));

    List<PhotoModel> photos = ImmutableList.of(
        new PhotoModel("Pic1", "http://fake.com/1.jpg", "A pic", "image/jpg", "p1", "id1", false),
        new PhotoModel(
            "Pic2", "https://fake.com/pic.png", "fine art", "image/png", "p2", "id1", false));

    ContainerResource data = new MediaContainerResource(albums, photos, null /*video*/);

    String serialized = objectMapper.writeValueAsString(data);

    ContainerResource deserializedModel =
        objectMapper.readValue(serialized, ContainerResource.class);

    Truth.assertThat(deserializedModel).isNotNull();
    Truth.assertThat(deserializedModel).isInstanceOf(MediaContainerResource.class);
    MediaContainerResource deserialized = (MediaContainerResource) deserializedModel;
    Truth.assertThat(deserialized.getAlbums()).hasSize(1);
    Truth.assertThat(deserialized.getPhotos()).hasSize(2);
    Truth.assertThat(deserialized).isEqualTo(data);
  }

  @Test
  public void verifyTransmogrifyAlbums_nullName() throws Exception {
    TransmogrificationConfig config = new TransmogrificationConfig() {
      public int getAlbumMaxSize() {
        return 2;
      }
    };
    List<MediaAlbum> albums = ImmutableList.of(new MediaAlbum("id1", null, "This is a fake album"));

    List<PhotoModel> photos = ImmutableList.of(
        new PhotoModel("Pic1", "http://fake.com/1.jpg", "A pic", "image/jpg", "p1", "id1", false));

    MediaContainerResource data = new MediaContainerResource(albums, photos, null /*video*/);
    data.transmogrify(config);
    Truth.assertThat(Iterables.get(data.getAlbums(), 0).getName()).isEqualTo(null);
  }

  @Test
  public void verifyTransmogrifyAlbums_NoLimit() throws Exception {
    TransmogrificationConfig config = new TransmogrificationConfig();
    List<MediaAlbum> albums =
        ImmutableList.of(new MediaAlbum("id1", "albumb1", "This is a fake album"));

    List<PhotoModel> photos = ImmutableList.of(
        new PhotoModel("Pic1", "http://fake.com/1.jpg", "A pic", "image/jpg", "p1", "id1", false),
        new PhotoModel("Pic3", "http://fake.com/2.jpg", "A pic", "image/jpg", "p3", "id1", false),
        new PhotoModel(
            "Pic2", "https://fake.com/pic.png", "fine art", "image/png", "p2", "id1", false));

    MediaContainerResource data = new MediaContainerResource(albums, photos, null /*video*/);
    data.transmogrify(config);
    Truth.assertThat(data.getAlbums()).hasSize(1);
    Truth.assertThat(data.getPhotos()).hasSize(3);
  }

  @Test
  public void verifyTransmogrifyAlbums_NoRootPhotos() throws Exception {
    TransmogrificationConfig config = new TransmogrificationConfig() {
      public boolean getAlbumAllowRootPhotos() {
        return false;
      }
    };
    List<MediaAlbum> albums =
        ImmutableList.of(new MediaAlbum("id1", "albumb1", "This is a fake album"));

    List<PhotoModel> photos = ImmutableList.of(
        new PhotoModel("Pic1", "http://fake.com/1.jpg", "A pic", "image/jpg", "p1", "id1", false),
        new PhotoModel("Pic3", "http://fake.com/2.jpg", "A pic", "image/jpg", "p3", "id1", false),
        new PhotoModel(
            "Pic2", "https://fake.com/pic.png", "fine art", "image/png", "p2", null, false));

    MediaContainerResource data = new MediaContainerResource(albums, photos, null /*video*/);
    data.transmogrify(config);
    Truth.assertThat(data.getAlbums()).hasSize(2);
    Truth.assertThat(data.getPhotos()).hasSize(3);
  }

  @Test
  public void verifyTransmogrifyAlbums_NameForbiddenCharacters() throws Exception {
    TransmogrificationConfig config = new TransmogrificationConfig() {
      public String getAlbumNameForbiddenCharacters() {
        return ":!";
      }

      public char getAlbumNameReplacementCharacter() {
        return '?';
      }
    };
    List<MediaAlbum> albums =
        ImmutableList.of(new MediaAlbum("id1", "This:a fake album!", "This:a fake album!"));
    List<PhotoModel> photos = ImmutableList.of(
        new PhotoModel("Pic1", "http://fake.com/1.jpg", "A pic", "image/jpg", "p1", "id1", false),
        new PhotoModel("Pic3", "http://fake.com/2.jpg", "A pic", "image/jpg", "p3", "id1", false),
        new PhotoModel(
            "Pic2", "https://fake.com/pic2.png", "fine art", "image/png", "p2", null, false),
        new PhotoModel(
            "Pic5", "https://fake.com/pic5.png", "fine art", "image/png", "p5", null, false),
        new PhotoModel(
            "Pic6", "https://fake.com/pic6.png", "fine art", "image/png", "p6", null, false));

    MediaContainerResource data = new MediaContainerResource(albums, photos, null /*video*/);
    data.transmogrify(config);
    Truth.assertThat(Iterables.get(data.getAlbums(), 0).getName()).isEqualTo("This?a fake album?");
  }

  @Test
  public void verifyTransmogrifyAlbums_NameNoForbiddenCharacters() throws Exception {
    TransmogrificationConfig config = new TransmogrificationConfig();
    List<MediaAlbum> albums =
        ImmutableList.of(new MediaAlbum("id1", "This:a fake album!", "This:a fake album!"));

    List<PhotoModel> photos = ImmutableList.of(
        new PhotoModel("Pic1", "http://fake.com/1.jpg", "A pic", "image/jpg", "p1", "id1", false),
        new PhotoModel("Pic3", "http://fake.com/2.jpg", "A pic", "image/jpg", "p3", "id1", false),
        new PhotoModel(
            "Pic2", "https://fake.com/pic.png", "fine art", "image/png", "p2", null, false));

    MediaContainerResource data = new MediaContainerResource(albums, photos, null /*video*/);
    data.transmogrify(config);
    Truth.assertThat(Iterables.get(data.getAlbums(), 0).getName()).isEqualTo("This:a fake album!");
  }

  @Test
  public void verifyTransmogrifyAlbums_stripName() throws Exception {
    TransmogrificationConfig config = new TransmogrificationConfig();
    List<MediaAlbum> albums =
        ImmutableList.of(new MediaAlbum("id1", "This:a fake album!   ", "This:a fake album!"));

    List<PhotoModel> photos = ImmutableList.of(
        new PhotoModel("Pic1", "http://fake.com/1.jpg", "A pic", "image/jpg", "p1", "id1", false),
        new PhotoModel("Pic3", "http://fake.com/2.jpg", "A pic", "image/jpg", "p3", "id1", false),
        new PhotoModel(
            "Pic2", "https://fake.com/pic.png", "fine art", "image/png", "p2", null, false));

    MediaContainerResource data = new MediaContainerResource(albums, photos, null /*video*/);
    data.transmogrify(config);
    Truth.assertThat(Iterables.get(data.getAlbums(), 0).getName()).isEqualTo("This:a fake album!");
  }

  @Test
  public void verifyTransmogrifyAlbums_NameTooLong() throws Exception {
    TransmogrificationConfig config = new TransmogrificationConfig() {
      public int getAlbumNameMaxLength() {
        return 5;
      }
    };
    List<MediaAlbum> albums =
        ImmutableList.of(new MediaAlbum("id1", "This:a fake album!", "This:a fake album!"));

    List<PhotoModel> photos = ImmutableList.of(
        new PhotoModel("Pic1", "http://fake.com/1.jpg", "A pic", "image/jpg", "p1", "id1", false),
        new PhotoModel("Pic3", "http://fake.com/2.jpg", "A pic", "image/jpg", "p3", "id1", false),
        new PhotoModel(
            "Pic2", "https://fake.com/pic.png", "fine art", "image/png", "p2", null, false));

    MediaContainerResource data = new MediaContainerResource(albums, photos, null /*video*/);
    data.transmogrify(config);
    Truth.assertThat(Iterables.get(data.getAlbums(), 0).getName()).hasLength(5);
  }

  @Test
  public void verifyTransmogrifyAlbums_NameNoLengthLimit() throws Exception {
    TransmogrificationConfig config = new TransmogrificationConfig();
    List<MediaAlbum> albums =
        ImmutableList.of(new MediaAlbum("id1", "albumb1", "This:a fake album!"));

    List<PhotoModel> photos = ImmutableList.of(
        new PhotoModel("Pic1", "http://fake.com/1.jpg", "A pic", "image/jpg", "p1", "id1", false),
        new PhotoModel("Pic3", "http://fake.com/2.jpg", "A pic", "image/jpg", "p3", "id1", false),
        new PhotoModel(
            "Pic2", "https://fake.com/pic.png", "fine art", "image/png", "p2", null, false));

    MediaContainerResource data = new MediaContainerResource(albums, photos, null /*video*/);
    data.transmogrify(config);
    Truth.assertThat(Iterables.get(data.getAlbums(), 0).getName()).hasLength(7);
  }

  @Test
  public void verifyTransmogrifyPhotos_TitleForbiddenCharacters() throws Exception {
    TransmogrificationConfig config = new TransmogrificationConfig() {
      public String getPhotoTitleForbiddenCharacters() {
        return ":!";
      }

      public char getPhotoTitleReplacementCharacter() {
        return '?';
      }
    };
    List<MediaAlbum> albums =
        ImmutableList.of(new MediaAlbum("id1", "albumb1", "This:a fake album!"));

    List<PhotoModel> photos = ImmutableList.of(
        new PhotoModel("Pic1!", "http://fake.com/1.jpg", "A pic", "image/jpg", "p1", "id1", false),
        new PhotoModel("Pic:3", "http://fake.com/2.jpg", "A pic", "image/jpg", "p3", "id1", false),
        new PhotoModel(
            "Pic2", "https://fake.com/pic.png", "fine art", "image/png", "p2", null, false));

    MediaContainerResource data = new MediaContainerResource(albums, photos, null /*video*/);
    data.transmogrify(config);
    Truth.assertThat(Iterables.get(data.getPhotos(), 0).getTitle()).isEqualTo("Pic1?");
    Truth.assertThat(Iterables.get(data.getPhotos(), 1).getTitle()).isEqualTo("Pic?3");
    Truth.assertThat(Iterables.get(data.getPhotos(), 2).getTitle()).isEqualTo("Pic2");
  }

  @Test
  public void verifyTransmogrifyPhotos_TitleNoForbiddenCharacters() throws Exception {
    TransmogrificationConfig config = new TransmogrificationConfig();
    List<MediaAlbum> albums =
        ImmutableList.of(new MediaAlbum("id1", "albumb1", "This:a fake album!"));

    List<PhotoModel> photos = ImmutableList.of(
        new PhotoModel("Pic?1", "http://fake.com/1.jpg", "A pic", "image/jpg", "p1", "id1", false),
        new PhotoModel("Pic:3", "http://fake.com/2.jpg", "A pic", "image/jpg", "p3", "id1", false),
        new PhotoModel(
            "Pic2", "https://fake.com/pic.png", "fine art", "image/png", "p2", null, false));

    MediaContainerResource data = new MediaContainerResource(albums, photos, null /*video*/);
    data.transmogrify(config);
    Truth.assertThat(Iterables.get(data.getPhotos(), 0).getTitle()).isEqualTo("Pic?1");
    Truth.assertThat(Iterables.get(data.getPhotos(), 1).getTitle()).isEqualTo("Pic:3");
    Truth.assertThat(Iterables.get(data.getPhotos(), 2).getTitle()).isEqualTo("Pic2");
  }

  @Test
  public void verifyTransmogrifyPhotos_TitleTooLong() throws Exception {
    TransmogrificationConfig config = new TransmogrificationConfig() {
      public int getPhotoTitleMaxLength() {
        return 3;
      }
    };
    List<MediaAlbum> albums =
        ImmutableList.of(new MediaAlbum("id1", "albumb1", "This:a fake album!"));

    List<PhotoModel> photos = ImmutableList.of(
        new PhotoModel("Pic1", "http://fake.com/1.jpg", "A pic", "image/jpg", "p1", "id1", false),
        new PhotoModel("Pic3", "http://fake.com/2.jpg", "A pic", "image/jpg", "p3", "id1", false),
        new PhotoModel(
            "P2", "https://fake.com/pic.png", "fine art", "image/png", "p2", null, false));

    MediaContainerResource data = new MediaContainerResource(albums, photos, null /*video*/);
    data.transmogrify(config);
    Truth.assertThat(Iterables.get(data.getPhotos(), 0).getTitle()).hasLength(3);
    Truth.assertThat(Iterables.get(data.getPhotos(), 1).getTitle()).hasLength(3);
    Truth.assertThat(Iterables.get(data.getPhotos(), 2).getTitle()).isEqualTo("P2");
  }

  @Test
  public void verifyTransmogrifyPhotos_TitleNoLengthLimit() throws Exception {
    TransmogrificationConfig config = new TransmogrificationConfig();
    List<MediaAlbum> albums =
        ImmutableList.of(new MediaAlbum("id1", "albumb1", "This:a fake album!"));

    List<PhotoModel> photos = ImmutableList.of(
        new PhotoModel("Pic1", "http://fake.com/1.jpg", "A pic", "image/jpg", "p1", "id1", false),
        new PhotoModel("Pic3", "http://fake.com/2.jpg", "A pic", "image/jpg", "p3", "id1", false),
        new PhotoModel(
            "Pic2", "https://fake.com/pic.png", "fine art", "image/png", "p2", null, false));

    MediaContainerResource data = new MediaContainerResource(albums, photos, null /*video*/);
    data.transmogrify(config);
    Truth.assertThat(Iterables.get(data.getPhotos(), 0).getTitle()).hasLength(4);
    Truth.assertThat(Iterables.get(data.getPhotos(), 1).getTitle()).hasLength(4);
    Truth.assertThat(Iterables.get(data.getPhotos(), 2).getTitle()).hasLength(4);
  }

  @Test
  public void verifyTransmogrifyPhotos_stripTitle() throws Exception {
    TransmogrificationConfig config = new TransmogrificationConfig();
    List<MediaAlbum> albums =
        ImmutableList.of(new MediaAlbum("id1", "albumb1", "This:a fake album!"));

    List<PhotoModel> photos = ImmutableList.of(
        new PhotoModel("Pic1 ", "http://fake.com/1.jpg", "A pic", "image/jpg", "p1", "id1", false),
        new PhotoModel("Pic3 ", "http://fake.com/2.jpg", "A pic", "image/jpg", "p3", "id1", false));

    MediaContainerResource data = new MediaContainerResource(albums, photos, null /*video*/);
    data.transmogrify(config);
    Truth.assertThat(Iterables.get(data.getPhotos(), 0).getTitle()).isEqualTo("Pic1");
    Truth.assertThat(Iterables.get(data.getPhotos(), 1).getTitle()).isEqualTo("Pic3");
  }

  @Test
  public void verifyPhotoToMediaContainer() throws Exception {
    TransmogrificationConfig config = new TransmogrificationConfig();
    List<PhotoAlbum> givenPhotoAlbum =
        ImmutableList.of(new PhotoAlbum("id1", "albumb1", "This:a fake album!"));
    List<MediaAlbum> expectedMediaAlbum =
        ImmutableList.of(new MediaAlbum("id1", "albumb1", "This:a fake album!"));
    // Ensure our _actual_ test below even makes sense; if this breaks that means we have caused api
    // incompatability between media and photo album, which was not somethin we've expected.
    Truth.assertThat(givenPhotoAlbum.stream().map(a -> a.getId()).collect(Collectors.toList()))
        .containsExactlyElementsIn(
            expectedMediaAlbum.stream().map(a -> a.getId()).collect(Collectors.toList()));
    Truth.assertThat(givenPhotoAlbum.stream().map(a -> a.getName()).collect(Collectors.toList()))
        .containsExactlyElementsIn(
            expectedMediaAlbum.stream().map(a -> a.getName()).collect(Collectors.toList()));
    Truth
        .assertThat(
            givenPhotoAlbum.stream().map(a -> a.getDescription()).collect(Collectors.toList()))
        .containsExactlyElementsIn(
            expectedMediaAlbum.stream().map(a -> a.getDescription()).collect(Collectors.toList()));

    List<PhotoModel> photos = ImmutableList.of(
        new PhotoModel("Pic1 ", "http://fake.com/1.jpg", "A pic", "image/jpg", "p1", "id1", false),
        new PhotoModel("Pic3 ", "http://fake.com/2.jpg", "A pic", "image/jpg", "p3", "id1", false));
    PhotosContainerResource photoData = new PhotosContainerResource(givenPhotoAlbum, photos);
    MediaContainerResource generatedMediaData = MediaContainerResource.photoToMedia(photoData);

    Truth.assertThat(generatedMediaData.getAlbums()).containsExactlyElementsIn(expectedMediaAlbum);
    Truth.assertThat(generatedMediaData.getPhotos()).containsExactlyElementsIn(photos);
    Truth.assertThat(generatedMediaData.getVideos()).isEmpty();
  }

  @Test
  public void verifyMediaToPhotoContainer() throws Exception {
    TransmogrificationConfig config = new TransmogrificationConfig();
    List<MediaAlbum> albums =
        ImmutableList.of(new MediaAlbum("id1", "albumb1", "This:a fake album!"));
    List<PhotoModel> photos = ImmutableList.of(
        new PhotoModel("Pic1 ", "http://fake.com/1.jpg", "A pic", "image/jpg", "p1", "id1", false),
        new PhotoModel("Pic3 ", "http://fake.com/2.jpg", "A pic", "image/jpg", "p3", "id1", false));
    // TODO(#1060) populate video content here to show that MediaContainerResource#photoToMedia
    // doesn't care.
    MediaContainerResource data = new MediaContainerResource(albums, photos, null /*video*/);

    PhotosContainerResource generatedPhotoData = MediaContainerResource.mediaToPhoto(data);
    Truth
        .assertThat(generatedPhotoData.getAlbums()
            .stream()
            .map(a -> a.getId())
            .collect(Collectors.toList()))
        .containsExactlyElementsIn(
            albums.stream().map(a -> a.getId()).collect(Collectors.toList()));
    Truth
        .assertThat(generatedPhotoData.getAlbums()
            .stream()
            .map(a -> a.getName())
            .collect(Collectors.toList()))
        .containsExactlyElementsIn(
            albums.stream().map(a -> a.getName()).collect(Collectors.toList()));
    Truth
        .assertThat(generatedPhotoData.getAlbums()
            .stream()
            .map(a -> a.getDescription())
            .collect(Collectors.toList()))
        .containsExactlyElementsIn(
            albums.stream().map(a -> a.getDescription()).collect(Collectors.toList()));
    Truth.assertThat(generatedPhotoData.getPhotos()).containsExactlyElementsIn(photos);
  }

  @Test
  public void verifyMediaToVideoContainer() {
    List<MediaAlbum> mediaAlbums =
        ImmutableList.of(new MediaAlbum("id1", "albumb1", "This:a fake album!"));
    List<VideoAlbum> videoAlbums =
        ImmutableList.of(new VideoAlbum("id1", "albumb1", "This:a fake album!"));
    List<VideoModel> videos = ImmutableList.of(
        new VideoModel(
            "Vid1", "http://fake.com/1.mp4", "A vid", "mediatype", "p1", "id1", false, null),
        new VideoModel(
            "Vid3", "http://fake.com/2.mp4", "A vid", "mediatype", "p3", "id1", false, null));
    MediaContainerResource data = new MediaContainerResource(mediaAlbums, null, videos);

    VideosContainerResource expected = new VideosContainerResource(videoAlbums, videos);
    VideosContainerResource actual = MediaContainerResource.mediaToVideo(data);
    assertEquals(expected, actual);
  }

  @Test
  public void verifyVideoToMediaContainer() {
    List<MediaAlbum> mediaAlbums =
        ImmutableList.of(new MediaAlbum("id1", "albumb1", "This:a fake album!"));
    List<VideoAlbum> videoAlbums =
        ImmutableList.of(new VideoAlbum("id1", "albumb1", "This:a fake album!"));
    List<VideoModel> videos = ImmutableList.of(
        new VideoModel(
            "Vid1", "http://fake.com/1.mp4", "A vid", "mediatype", "p1", "id1", false, null),
        new VideoModel(
            "Vid3", "http://fake.com/2.mp4", "A vid", "mediatype", "p3", "id1", false, null));
    VideosContainerResource data = new VideosContainerResource(videoAlbums, videos);

    MediaContainerResource expected = new MediaContainerResource(mediaAlbums, null, videos);
    MediaContainerResource actual = MediaContainerResource.videoToMedia(data);
    assertEquals(expected, actual);
  }
}
