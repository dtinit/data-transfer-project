package org.dataportabilityproject.types.transfer.models.photos;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.truth.Truth;
import org.dataportabilityproject.types.transfer.models.ContainerResource;
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
            PhotoModel.builder()
                .setTitle("Pic1")
                .setFetchableUrl("http://fake.com/1.jpg")
                .setDescription("A pic")
                .setMediaType("image/jpeg")
                .setAlbumId("id1")
                .build(),
            PhotoModel.builder()
                .setTitle("Pic1")
                .setFetchableUrl("http://fake.com/pic.jpg")
                .setDescription("fine art")
                .setMediaType("image/png")
                .setAlbumId("id1")
                .build());

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
