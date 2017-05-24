package org.dataportabilityproject.dataModels.tasks;

import com.google.common.collect.ImmutableList;
import java.util.Collection;
import org.dataportabilityproject.dataModels.ContinuationInformation;
import org.dataportabilityproject.dataModels.DataModel;

/**
 * A Wrapper for all the possible objects that can be returned by a task exporter.
 */
public class TaskModelWrapper implements DataModel {
  private final Collection<TaskListModel> lists;
  private final Collection<TaskModel> tasks;
  private final ContinuationInformation continuationInformation;

  public TaskModelWrapper(Collection<TaskListModel> lists, Collection<TaskModel> tasks,
      ContinuationInformation continuationInformation) {
    this.lists = lists == null ? ImmutableList.of() : lists;
    this.tasks = tasks == null ? ImmutableList.of() : tasks;
    this.continuationInformation = continuationInformation;
  }

  @Override
  public ContinuationInformation getContinuationInformation() {
    return continuationInformation;
  }

  public Collection<TaskListModel> getLists() {
    return lists;
  }

  public Collection<TaskModel> getTasks() {
    return tasks;
  }
}
