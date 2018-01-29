package org.dataportabilityproject.types.transfer.models.mail;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.truth.Truth;
import java.util.List;
import org.dataportabilityproject.types.transfer.models.DataModel;
import org.junit.Test;

public class MailModelWrapperTest {
  @Test
  public void verifySerializeDeserialize() throws Exception {
    ObjectMapper objectMapper = new ObjectMapper();

    List<MailContainerModel> containers = ImmutableList.of(
        new MailContainerModel("id1", "container1"),
        new MailContainerModel("id2", "container2")
    );

    List<MailMessageModel> messages = ImmutableList.of(
        new MailMessageModel("foo", ImmutableList.of("1")),
        new MailMessageModel("bar", ImmutableList.of("1", "2'"))
    );

    DataModel data = new MailModelWrapper(containers, messages);

    String serialized = objectMapper.writeValueAsString(data);

    DataModel deserializedModel = objectMapper.readValue(serialized, DataModel.class);

    Truth.assertThat(deserializedModel).isNotNull();
    Truth.assertThat(deserializedModel).isInstanceOf(MailModelWrapper.class);
    MailModelWrapper deserialized = (MailModelWrapper) deserializedModel;
    Truth.assertThat(deserialized.getMessages()).hasSize(2);
    Truth.assertThat(deserialized.getFolders()).hasSize(2);
  }
}
