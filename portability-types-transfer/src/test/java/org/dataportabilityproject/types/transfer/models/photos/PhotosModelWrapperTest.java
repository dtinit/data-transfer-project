package org.dataportabilityproject.types.transfer.models.photos;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.truth.Truth;
import java.util.List;
import org.dataportabilityproject.types.transfer.models.DataModel;
import org.junit.Test;

public class PhotosModelWrapperTest {
  @Test
  public void verifySerializeDeserialize() throws Exception {
    ObjectMapper objectMapper = new ObjectMapper();

    List<PhotoAlbum> albums = ImmutableList.of(
        new PhotoAlbum("id1", "albumb1", "This is a fake albumb")
    );

    List<PhotoModel> photos = ImmutableList.of(
        new PhotoModel("Pic1", "http://fake.com/1.jpg", "A pic", "image/jpg", "id1"),
        new PhotoModel("Pic2", "https://fake.com/pic.png", "fine art", "image/png", "id1")
    );

    DataModel data = new PhotosModelWrapper(albums, photos);

    String serialized = objectMapper.writeValueAsString(data);

    DataModel deserializedModel = objectMapper.readValue(serialized, DataModel.class);

    Truth.assertThat(deserializedModel).isNotNull();
    Truth.assertThat(deserializedModel).isInstanceOf(PhotosModelWrapper.class);
    PhotosModelWrapper deserialized = (PhotosModelWrapper) deserializedModel;
    Truth.assertThat(deserialized.getAlbums()).hasSize(1);
    Truth.assertThat(deserialized.getPhotos()).hasSize(2);
  }
}
