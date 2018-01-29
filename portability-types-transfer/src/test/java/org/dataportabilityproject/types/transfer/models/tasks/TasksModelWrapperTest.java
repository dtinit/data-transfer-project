package org.dataportabilityproject.types.transfer.models.tasks;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.truth.Truth;
import java.util.List;
import org.dataportabilityproject.types.transfer.models.DataModel;
import org.junit.Test;

public class TasksModelWrapperTest {
  @Test
  public void verifySerializeDeserialize() throws Exception {
    ObjectMapper objectMapper = new ObjectMapper();

    List<TaskListModel> taskLists = ImmutableList.of(
        new TaskListModel("id1", "List 1")
    );

    List<TaskModel> tasks = ImmutableList.of(
        new TaskModel("id1", "Write Better tests", "Do this soon"),
        new TaskModel("id1", "Liberate all the data", "do this in stages")
    );

    DataModel data = new TaskModelWrapper(taskLists, tasks);

    String serialized = objectMapper.writeValueAsString(data);

    DataModel deserializedModel = objectMapper.readValue(serialized, DataModel.class);

    Truth.assertThat(deserializedModel).isNotNull();
    Truth.assertThat(deserializedModel).isInstanceOf(TaskModelWrapper.class);
    TaskModelWrapper deserialized = (TaskModelWrapper) deserializedModel;
    Truth.assertThat(deserialized.getLists()).hasSize(1);
    Truth.assertThat(deserialized.getTasks()).hasSize(2);
  }
}
