/*
 * Copyright 2018 The Data Transfer Project Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.datatransferproject.transfer.rememberthemilk.tasks;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.spi.transfer.idempotentexecutor.IdempotentImportExecutor;
import org.datatransferproject.spi.transfer.provider.ImportResult;
import org.datatransferproject.spi.transfer.provider.Importer;
import org.datatransferproject.transfer.rememberthemilk.model.tasks.TaskSeries;
import org.datatransferproject.types.common.models.tasks.TaskContainerResource;
import org.datatransferproject.types.common.models.tasks.TaskListModel;
import org.datatransferproject.types.common.models.tasks.TaskModel;
import org.datatransferproject.types.transfer.auth.AppCredentials;
import org.datatransferproject.types.transfer.auth.AuthData;
import org.datatransferproject.types.transfer.auth.TokenAuthData;
import java.io.IOException;
import java.util.UUID;

/*
 * Importer for Tasks data type to Remember The Milk Service.
 */
public class RememberTheMilkTasksImporter implements Importer<AuthData, TaskContainerResource> {

    private final AppCredentials appCredentials;

    private final Monitor monitor;

    private RememberTheMilkService service;

    public RememberTheMilkTasksImporter(AppCredentials appCredentials, Monitor monitor) {
        this.appCredentials = appCredentials;
        this.monitor = monitor;
        this.service = null;
    }

    @Override
    public ImportResult importItem(UUID jobId, IdempotentImportExecutor idempotentExecutor, AuthData authData, TaskContainerResource data) {
        String timeline;
        try {
            RememberTheMilkService service = getOrCreateService(authData);
            timeline = service.createTimeline();
            for (TaskListModel taskList : data.getLists()) {
                idempotentExecutor.executeAndSwallowIOExceptions(taskList.getId(), taskList.getName(), () -> service.createTaskList(taskList.getName(), timeline).id);
            }
            for (TaskModel task : data.getTasks()) {
                // Empty or blank tasks aren't valid in RTM
                if (!Strings.isNullOrEmpty(task.getText())) {
                    idempotentExecutor.executeAndSwallowIOExceptions(Integer.toString(task.hashCode()), task.getText(), () -> {
                        String newList = idempotentExecutor.getCachedValue(task.getTaskListId());
                        return insertTask(task, newList, timeline);
                    });
                }
            }
        } catch (Exception e) {
            monitor.severe(() -> "Error importing item", e);
            return new ImportResult(e);
        }
        return new ImportResult(ImportResult.ResultType.OK);
    }

    private Integer insertTask(TaskModel task, String newList, String timeline) throws IOException {
        TaskSeries addedTask = service.createTask(task.getText(), timeline, newList);
        // todo: add notes
        if (task.getCompletedTime() != null) {
            // NB: this assumes that only one task was added above, and that the task series
            // in the response contains only one task.
            // TODO: Address recurring events where some are completed and some are not
            service.completeTask(timeline, newList, addedTask.id, addedTask.tasks.get(0).id);
        }
        if (task.getDueTime() != null) {
            // TODO: Address recurring events with different due dates/times
            service.setDueDate(timeline, newList, addedTask.id, addedTask.tasks.get(0).id, task.getDueTime());
        }
        return addedTask.id;
    }

    private RememberTheMilkService getOrCreateService(AuthData authData) {
        Preconditions.checkArgument(authData instanceof TokenAuthData);
        return service == null ? createService((TokenAuthData) authData) : service;
    }

    private RememberTheMilkService createService(TokenAuthData authData) {
        return new RememberTheMilkService(new RememberTheMilkSignatureGenerator(appCredentials, authData.getToken()));
    }
}
