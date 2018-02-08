package org.dataportabilityproject.types.transfer.models.mail;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.truth.Truth;
import java.util.List;
import org.dataportabilityproject.types.transfer.models.ContainerResource;
import org.junit.Test;

public class MailContainerResourceTest {
  @Test
  public void verifySerializeDeserialize() throws Exception {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerSubtypes(MailContainerResource.class);

    List<MailContainerModel> containers = ImmutableList.of(
        new MailContainerModel("id1", "container1"),
        new MailContainerModel("id2", "container2")
    );

    List<MailMessageModel> messages = ImmutableList.of(
        new MailMessageModel("foo", ImmutableList.of("1")),
        new MailMessageModel("bar", ImmutableList.of("1", "2'"))
    );

    ContainerResource data = new MailContainerResource(containers, messages);

    String serialized = objectMapper.writeValueAsString(data);

    ContainerResource deserializedModel = objectMapper.readValue(serialized, ContainerResource.class);

    Truth.assertThat(deserializedModel).isNotNull();
    Truth.assertThat(deserializedModel).isInstanceOf(MailContainerResource.class);
    MailContainerResource deserialized = (MailContainerResource) deserializedModel;
    Truth.assertThat(deserialized.getMessages()).hasSize(2);
    Truth.assertThat(deserialized.getFolders()).hasSize(2);
  }
}
