package org.dataportabilityproject.serviceProviders.google.tasks;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.services.tasks.Tasks;
import com.google.api.services.tasks.model.Task;
import com.google.api.services.tasks.model.TaskList;
import com.google.api.services.tasks.model.TaskLists;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.dataportabilityproject.dataModels.ContinuationInformation;
import org.dataportabilityproject.dataModels.ExportInformation;
import org.dataportabilityproject.dataModels.Exporter;
import org.dataportabilityproject.dataModels.Importer;
import org.dataportabilityproject.dataModels.PaginationInformation;
import org.dataportabilityproject.dataModels.Resource;
import org.dataportabilityproject.dataModels.tasks.TaskListModel;
import org.dataportabilityproject.dataModels.tasks.TaskModel;
import org.dataportabilityproject.dataModels.tasks.TaskModelWrapper;
import org.dataportabilityproject.jobDataCache.JobDataCache;
import org.dataportabilityproject.serviceProviders.google.GoogleStaticObjects;
import org.dataportabilityproject.shared.IdOnlyResource;
import org.dataportabilityproject.shared.StringPaginationToken;

public class GoogleTaskService implements
        Importer<TaskModelWrapper>,
        Exporter<TaskModelWrapper> {
    private static final long PAGE_SIZE = 50;
    private final Tasks taskClient;
    private final JobDataCache jobDataCache;

    public GoogleTaskService(Credential credential, JobDataCache jobDataCache) {
        taskClient = new Tasks.Builder(
                GoogleStaticObjects.getHttpTransport(), GoogleStaticObjects.JSON_FACTORY, credential)
                .setApplicationName(GoogleStaticObjects.APP_NAME)
                .build();
        this.jobDataCache = jobDataCache;
    }

    private TaskModelWrapper getTaskLists(Optional<PaginationInformation> pageInfo)
            throws IOException {
        Tasks.Tasklists.List query = taskClient.tasklists().list()
                .setMaxResults(PAGE_SIZE);
        if (pageInfo.isPresent()) {
            query.setPageToken(((StringPaginationToken) pageInfo.get()).getId());
        }
        TaskLists result = query.execute();
        List<TaskListModel> newTaskLists = new ArrayList<>(result.getItems().size());
        List<Resource> newResources = new ArrayList<>(result.getItems().size());
        for (TaskList taskList : result.getItems()) {
            newTaskLists.add(new TaskListModel(taskList.getId(), taskList.getTitle()));
            newResources.add(new IdOnlyResource(taskList.getId()));
        }
        PaginationInformation newPageInfo = null;

        if (result.getNextPageToken() != null) {
            newPageInfo = new StringPaginationToken(result.getNextPageToken());
        }

        return new TaskModelWrapper(
            newTaskLists,
            null,
            new ContinuationInformation(newResources, newPageInfo));
    }

    private TaskModelWrapper getTasks(String taskListId, Optional<PaginationInformation> pageInfo)
            throws IOException {
        com.google.api.services.tasks.model.Tasks result = null;

        Tasks.TasksOperations.List query = taskClient.tasks()
                .list(taskListId).setMaxResults(PAGE_SIZE);
        if (pageInfo.isPresent()) {
            query.setPageToken(((StringPaginationToken) pageInfo.get()).getId());
        }
        result = query.execute();
        List<TaskModel> newTasks = result.getItems().stream()
            .map(t -> new TaskModel(t.getId(), t.getTitle(), t.getNotes()))
            .collect(Collectors.toList());


        PaginationInformation newPageInfo = null;
        if (result.getNextPageToken() != null) {
            newPageInfo = new StringPaginationToken(result.getNextPageToken());
        }

        return new TaskModelWrapper(
            null,
            newTasks,
            new ContinuationInformation(null, newPageInfo));
    }

    @Override
    public void importItem(TaskModelWrapper wrapper) throws IOException {
        for(TaskListModel taskList : wrapper.getLists()) {
            TaskList newTaskList = new TaskList()
                .setTitle("Imported copy - " + taskList.getName());
            TaskList insertedTaskList = taskClient.tasklists().insert(newTaskList).execute();
            jobDataCache.store(taskList.getId(), insertedTaskList.getId());
        }
        for (TaskModel oldTask : wrapper.getTasks()) {
            Task newTask = new Task()
                    .setTitle(oldTask.getText())
                    .setNotes(oldTask.getNotes());
            String newTaskId = jobDataCache.getData(oldTask.getTaskListId(), String.class);
            taskClient.tasks().insert(newTaskId, newTask).execute();
        }
    }

    @Override
    public TaskModelWrapper export(ExportInformation exportInformation) throws IOException {
        if (exportInformation.getResource().isPresent()) {
            String taskListId = ((IdOnlyResource) exportInformation.getResource().get()).getId();
            return getTasks(taskListId, exportInformation.getPaginationInformation());
        } else {
            return getTaskLists(exportInformation.getPaginationInformation());
        }
    }
}
