package org.datatransferproject.types.transfer.models.photos;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.truth.Truth;
import org.datatransferproject.types.transfer.models.ContainerResource;
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
}
