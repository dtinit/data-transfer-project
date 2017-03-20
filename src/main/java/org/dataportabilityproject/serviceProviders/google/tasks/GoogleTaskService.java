package org.dataportabilityproject.serviceProviders.google.tasks;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.google.api.services.tasks.Tasks;
import com.google.api.services.tasks.model.Task;
import com.google.api.services.tasks.model.TaskList;
import com.google.api.services.tasks.model.TaskLists;
import com.google.common.collect.ImmutableList;
import org.dataportabilityproject.dataModels.Exporter;
import org.dataportabilityproject.dataModels.Importer;
import org.dataportabilityproject.serviceProviders.google.GoogleStaticObjects;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class GoogleTaskService implements
        Importer<org.dataportabilityproject.dataModels.tasks.TaskList>,
        Exporter<org.dataportabilityproject.dataModels.tasks.TaskList> {
    private static final long PAGE_SIZE = 50;
    private final Tasks taskClient;

    public GoogleTaskService(Credential credential) {
        taskClient = new Tasks.Builder(
                GoogleStaticObjects.getHttpTransport(), GoogleStaticObjects.JSON_FACTORY, credential)
                .setApplicationName(GoogleStaticObjects.APP_NAME)
                .build();
    }

    private ImmutableList<TaskList> getTaskLists() throws IOException {
        ImmutableList.Builder<TaskList> resultBuilder = ImmutableList.builder();

        TaskLists result = null;
        do {
            Tasks.Tasklists.List query = taskClient.tasklists().list()
                    .setMaxResults(Long.valueOf(PAGE_SIZE));
            if (result != null) {
                query.setPageToken(result.getNextPageToken());
            }
            result = query.execute();
            resultBuilder.addAll(result.getItems());
        } while (!result.getItems().isEmpty() && !Strings.isNullOrEmpty(result.getNextPageToken()));

        return resultBuilder.build();
    }

    private ImmutableList<Task> getTasks(TaskList taskList) throws IOException {
        ImmutableList.Builder<Task> resultBuilder = ImmutableList.builder();

        com.google.api.services.tasks.model.Tasks result = null;
        do {
            Tasks.TasksOperations.List query = taskClient.tasks()
                    .list(taskList.getId()).setMaxResults(Long.valueOf(PAGE_SIZE));
            if (result != null) {
                query.setPageToken(result.getNextPageToken());
            }
            result = query.execute();
            resultBuilder.addAll(result.getItems());
        } while (!result.getItems().isEmpty() && !Strings.isNullOrEmpty(result.getNextPageToken()));

        return resultBuilder.build();
    }

    @Override
    public void importItem(org.dataportabilityproject.dataModels.tasks.TaskList taskList) throws IOException {
        TaskList newTaskList = new TaskList()
                .setTitle("Imported copy - " + taskList.getName());
        TaskList insertedTaskList = taskClient.tasklists().insert(newTaskList).execute();
        for (org.dataportabilityproject.dataModels.tasks.Task oldTask : taskList.getTasks()) {
            Task newTask = new Task()
                    .setTitle(oldTask.getText())
                    .setNotes(oldTask.getNotes());
            taskClient.tasks().insert(insertedTaskList.getId(), newTask).execute();
        }
    }

    @Override
    public Collection<org.dataportabilityproject.dataModels.tasks.TaskList> export() throws IOException {
        ImmutableList.Builder<org.dataportabilityproject.dataModels.tasks.TaskList> results = ImmutableList.builder();
        for (TaskList list : getTaskLists()) {
            ImmutableList<Task> tasks = getTasks(list);
            List<org.dataportabilityproject.dataModels.tasks.Task> newTasks = tasks.stream()
                    .map(t -> new org.dataportabilityproject.dataModels.tasks.Task(t.getTitle(), t.getNotes()))
                    .collect(Collectors.toList());
            org.dataportabilityproject.dataModels.tasks.TaskList newList =
                    new org.dataportabilityproject.dataModels.tasks.TaskList(
                            list.getTitle(),
                            ImmutableList.copyOf(newTasks));
            results.add(newList);
        }
        return results.build();
    }
}
