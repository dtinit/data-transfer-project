package org.datatransferproject.types.common.models.photos;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.truth.Truth;
import org.datatransferproject.types.common.models.ContainerResource;
import org.junit.Test;

import java.util.List;

public class PhotosContainerResourceTest {
  @Test
  public void verifySerializeDeserialize() throws Exception {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerSubtypes(PhotosContainerResource.class);

    List<PhotoAlbum> albums =
        ImmutableList.of(new PhotoAlbum("id1", "albumb1", "This is a fake albumb"));

    List<PhotoModel> photos =
        ImmutableList.of(
            new PhotoModel("Pic1", "http://fake.com/1.jpg", "A pic", "image/jpg", "p1", "id1",
                false),
            new PhotoModel(
                "Pic2", "https://fake.com/pic.png", "fine art", "image/png", "p2", "id1", false));

    ContainerResource data = new PhotosContainerResource(albums, photos);

    String serialized = objectMapper.writeValueAsString(data);

    ContainerResource deserializedModel =
        objectMapper.readValue(serialized, ContainerResource.class);

    Truth.assertThat(deserializedModel).isNotNull();
    Truth.assertThat(deserializedModel).isInstanceOf(PhotosContainerResource.class);
    PhotosContainerResource deserialized = (PhotosContainerResource) deserializedModel;
    Truth.assertThat(deserialized.getAlbums()).hasSize(1);
    Truth.assertThat(deserialized.getPhotos()).hasSize(2);
  }

    @Test
  public void verifyTransmogrifyAlbums_evenDivision() throws Exception {
    List<PhotoAlbum> albums =
        ImmutableList.of(new PhotoAlbum("id1", "albumb1", "This is a fake album"));

    List<PhotoModel> photos =
        ImmutableList.of(
            new PhotoModel("Pic1", "http://fake.com/1.jpg", "A pic", "image/jpg", "p1", "id1",
                false),
            new PhotoModel("Pic3", "http://fake.com/2.jpg", "A pic", "image/jpg", "p3", "id1",
                false),
            new PhotoModel("Pic4", "http://fake.com/3.jpg", "A pic", "image/jpg", "p4", "id1",
                false),
            new PhotoModel(
                "Pic2", "https://fake.com/pic.png", "fine art", "image/png", "p2", "id1", false));

    PhotosContainerResource data = new PhotosContainerResource(albums, photos);
    data.transmogrifyAlbums(2);
    Truth.assertThat(data.getAlbums()).hasSize(2);
    Truth.assertThat(data.getPhotos()).hasSize(4);
  }

@Test
  public void verifyTransmogrifyAlbums_oddDivision() throws Exception {
    List<PhotoAlbum> albums =
        ImmutableList.of(new PhotoAlbum("id1", "albumb1", "This is a fake album"));

    List<PhotoModel> photos =
        ImmutableList.of(
            new PhotoModel("Pic1", "http://fake.com/1.jpg", "A pic", "image/jpg", "p1", "id1",
                false),
            new PhotoModel("Pic3", "http://fake.com/2.jpg", "A pic", "image/jpg", "p3", "id1",
                false),
            new PhotoModel(
                "Pic2", "https://fake.com/pic.png", "fine art", "image/png", "p2", "id1", false));

    PhotosContainerResource data = new PhotosContainerResource(albums, photos);
    data.transmogrifyAlbums(2);
    Truth.assertThat(data.getAlbums()).hasSize(2);
    Truth.assertThat(data.getPhotos()).hasSize(3);
  }

  @Test
  public void verifyTransmogrifyAlbums_oddDivisionWithLoosePhotos() throws Exception {
    List<PhotoAlbum> albums =
        ImmutableList.of(new PhotoAlbum("id1", "albumb1", "This is a fake album"));

    List<PhotoModel> photos =
        ImmutableList.of(
            new PhotoModel("Pic1", "http://fake.com/1.jpg", "A pic", "image/jpg", "p1", "id1",
                false),
            new PhotoModel("Pic3", "http://fake.com/2.jpg", "A pic", "image/jpg", "p3", "id1",
                false),
            new PhotoModel("Pic4", "http://fake.com/3.jpg", "A pic", "image/jpg", "p4", "id1",
                false),
            new PhotoModel(
                "Pic2", "https://fake.com/pic.png", "fine art", "image/png", "p2", null, false),
            new PhotoModel(
                "Pic5", "https://fake.com/pic.png", "fine art", "image/png", "p5", null, false),
            new PhotoModel(
                "Pic6", "https://fake.com/pic.png", "fine art", "image/png", "p6", null, false));

    PhotosContainerResource data = new PhotosContainerResource(albums, photos);
    data.transmogrifyAlbums(2);
    Truth.assertThat(data.getAlbums()).hasSize(2);
    Truth.assertThat(data.getPhotos()).hasSize(6);
  }

  @Test
  public void verifyTransmogrifyAlbums_NoLimit() throws Exception {
    List<PhotoAlbum> albums =
        ImmutableList.of(new PhotoAlbum("id1", "albumb1", "This is a fake album"));

    List<PhotoModel> photos =
        ImmutableList.of(
            new PhotoModel("Pic1", "http://fake.com/1.jpg", "A pic", "image/jpg", "p1", "id1",
                false),
            new PhotoModel("Pic3", "http://fake.com/2.jpg", "A pic", "image/jpg", "p3", "id1",
                false),
            new PhotoModel(
                "Pic2", "https://fake.com/pic.png", "fine art", "image/png", "p2", "id1", false));

    PhotosContainerResource data = new PhotosContainerResource(albums, photos);
    data.transmogrifyAlbums(-1);
    Truth.assertThat(data.getAlbums()).hasSize(1);
    Truth.assertThat(data.getPhotos()).hasSize(3);
  }

  @Test
  public void verifyTransmogrifyAlbums_NoRootPhotos() throws Exception {
    List<PhotoAlbum> albums =
        ImmutableList.of(new PhotoAlbum("id1", "albumb1", "This is a fake album"));

    List<PhotoModel> photos =
        ImmutableList.of(
            new PhotoModel("Pic1", "http://fake.com/1.jpg", "A pic", "image/jpg", "p1", "id1",
                false),
            new PhotoModel("Pic3", "http://fake.com/2.jpg", "A pic", "image/jpg", "p3", "id1",
                false),
            new PhotoModel(
                "Pic2", "https://fake.com/pic.png", "fine art", "image/png", "p2", null, false));

    PhotosContainerResource data = new PhotosContainerResource(albums, photos);
    data.transmogrifyAlbums(-1, false);
    Truth.assertThat(data.getAlbums()).hasSize(2);
    Truth.assertThat(data.getPhotos()).hasSize(3);
  }

}
