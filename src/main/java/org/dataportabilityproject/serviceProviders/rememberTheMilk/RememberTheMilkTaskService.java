package org.dataportabilityproject.serviceProviders.rememberTheMilk;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.google.api.client.xml.XmlNamespaceDictionary;
import com.google.api.client.xml.XmlObjectParser;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.dataportabilityproject.dataModels.Exporter;
import org.dataportabilityproject.dataModels.Importer;
import org.dataportabilityproject.serviceProviders.rememberTheMilk.model.GetListResponse;
import org.dataportabilityproject.serviceProviders.rememberTheMilk.model.GetListsResponse;
import org.dataportabilityproject.serviceProviders.rememberTheMilk.model.ListAddResponse;
import org.dataportabilityproject.serviceProviders.rememberTheMilk.model.ListInfo;
import org.dataportabilityproject.serviceProviders.rememberTheMilk.model.RememberTheMilkResponse;
import org.dataportabilityproject.serviceProviders.rememberTheMilk.model.Task;
import org.dataportabilityproject.serviceProviders.rememberTheMilk.model.TaskAddResponse;
import org.dataportabilityproject.serviceProviders.rememberTheMilk.model.TaskList;
import org.dataportabilityproject.serviceProviders.rememberTheMilk.model.TaskSeries;
import org.dataportabilityproject.serviceProviders.rememberTheMilk.model.TimelineCreateResponse;
import org.dataportabilityproject.shared.IOInterface;

import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkState;

/**
 * List the lists that exist in RTM.
 */
public class RememberTheMilkTaskService implements
        Importer<org.dataportabilityproject.dataModels.tasks.TaskList>,
        Exporter<org.dataportabilityproject.dataModels.tasks.TaskList> {
    private static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();
    private final String authToken;
    private final String apiKey;
    private final org.dataportabilityproject.serviceProviders.rememberTheMilk.RememberTheMilkSignatureGenerator signatureGenerator;

    public RememberTheMilkTaskService(String secret, String apiKey, IOInterface ioInterface) throws IOException {
        this.apiKey = apiKey;
        this.signatureGenerator = new org.dataportabilityproject.serviceProviders.rememberTheMilk.RememberTheMilkSignatureGenerator(secret);
        org.dataportabilityproject.serviceProviders.rememberTheMilk.TokenGenerator tokenGenerator = new org.dataportabilityproject.serviceProviders.rememberTheMilk.TokenGenerator(this.signatureGenerator, ioInterface, apiKey);
        this.authToken = tokenGenerator.getToken();
    }

    public RememberTheMilkTaskService(String secret, String apiKey, String authToken) throws IOException {
        this.apiKey = apiKey;
        this.signatureGenerator = new org.dataportabilityproject.serviceProviders.rememberTheMilk.RememberTheMilkSignatureGenerator(secret);
        this.authToken = authToken;
    }

    private GetListsResponse getLists() throws IOException {
        return makeRequest(org.dataportabilityproject.serviceProviders.rememberTheMilk.RememberTheMilkMethods.LISTS_GET_LIST, ImmutableMap.of(), GetListsResponse.class);
    }

    private GetListResponse getList(int id) throws IOException{
        return makeRequest(org.dataportabilityproject.serviceProviders.rememberTheMilk.RememberTheMilkMethods.TASKS_GET_LIST,
                ImmutableMap.of("list_id", Long.toString(id)),
                GetListResponse.class);
    }

    @Override
    public void importItem(org.dataportabilityproject.dataModels.tasks.TaskList tasklist) throws IOException {
        String timeline = createTimeline();
        ListInfo listInfo = createTaskList(tasklist.getName(), timeline);
        for (org.dataportabilityproject.dataModels.tasks.Task taskToAdd : tasklist.getTasks()) {
            TaskSeries addedTask = createTask(taskToAdd.getText(), timeline, listInfo.id);
            // TODO add note here
        }
    }

    @Override
    public Collection<org.dataportabilityproject.dataModels.tasks.TaskList> export() throws IOException {
        ImmutableList.Builder<org.dataportabilityproject.dataModels.tasks.TaskList> result = ImmutableList.builder();

        for (ListInfo oldListInfo : getLists().listInfoList.lists) {
            if (oldListInfo.name.equals("All Tasks")) {
                // All Tasks is a special list that contains everything,
                // don't copy that over.
                continue;
            }
            ImmutableList.Builder<org.dataportabilityproject.dataModels.tasks.Task> newTasksBuilder =
                    ImmutableList.builder();
            GetListResponse oldList = getList(oldListInfo.id);
            List<TaskList> taskLists = oldList.tasks.list;
            for (TaskList taskList : taskLists) {
                if (taskList.taskSeriesList != null) {
                    for (TaskSeries taskSeries : taskList.taskSeriesList) {
                        newTasksBuilder.add(new org.dataportabilityproject.dataModels.tasks.Task(
                                taskSeries.name,
                                taskSeries.notes.toString()));
                        for (Task task : taskSeries.tasks) {
                            // Do something here with completion date, but its odd there can be more than one.
                        }
                    }
                }
            }
            result.add(new org.dataportabilityproject.dataModels.tasks.TaskList(
                    oldListInfo.name,
                    newTasksBuilder.build()));
        }

        return result.build();
    }

    private String createTimeline() throws IOException {
        TimelineCreateResponse timelineCreateResponse =
                makeRequest(org.dataportabilityproject.serviceProviders.rememberTheMilk.RememberTheMilkMethods.TIMELINES_CREATE, ImmutableMap.of(), TimelineCreateResponse.class);
        checkState(!Strings.isNullOrEmpty(timelineCreateResponse.timeline));
        return timelineCreateResponse.timeline;
    }

    private ListInfo createTaskList(String name, String timeline) throws IOException {
        Map<String, String> params = ImmutableMap.of(
                "timeline", timeline,
                "name", ("Copy of: " + name)
        );
        ListAddResponse response = makeRequest(org.dataportabilityproject.serviceProviders.rememberTheMilk.RememberTheMilkMethods.LISTS_ADD, params, ListAddResponse.class);
        checkState(response.listInfo != null, "Added list is null");
        checkState(response.listInfo.id != 0, "Added list has id of zero");
        return response.listInfo;
    }

    private TaskSeries createTask(String name, String timeline, long listId) throws IOException {
        Map<String, String> params = ImmutableMap.of(
                "timeline", timeline,
                "name", name,
                "list_id", Long.toString(listId)
        );
        TaskAddResponse taskAddResponse = makeRequest(org.dataportabilityproject.serviceProviders.rememberTheMilk.RememberTheMilkMethods.TASK_ADD, params, TaskAddResponse.class);
        return taskAddResponse.taskList.taskSeriesList.get(0);
    }

    private <T extends RememberTheMilkResponse> T makeRequest(org.dataportabilityproject.serviceProviders.rememberTheMilk.RememberTheMilkMethods method,
                                                              Map<String, String> parameters,
                                                              Class<T> dataClass) throws IOException {

        StringBuilder parameterString = new StringBuilder();
        for (String key : parameters.keySet()) {
            parameterString
                    .append("&")
                    .append(key)
                    .append("=")
                    .append(parameters.get(key));
        }

        URL url = new URL(method.getUrl()
                + "&api_key=" + apiKey
                + "&auth_token=" + authToken
                + parameterString);
        String signature = signatureGenerator.getSignature(url);
        URL signedUrl = new URL(url + "&api_sig=" + signature);

        HttpRequestFactory requestFactory = HTTP_TRANSPORT.createRequestFactory();
        HttpRequest getRequest = requestFactory.buildGetRequest(new GenericUrl(signedUrl));
        getRequest.setParser(new XmlObjectParser(new XmlNamespaceDictionary().set("", "")));
        HttpResponse response = getRequest.execute();
        int statusCode = response.getStatusCode();
        if (statusCode != 200) {
            throw new IOException("Bad status code: " + statusCode + " error: " + response.getStatusMessage());
        }

        T parsedResponse = response.parseAs(dataClass);

        if (parsedResponse.error != null) {
            throw new IOException("Error making call to " + signedUrl + " error: " + parsedResponse.error);
        }

        return parsedResponse;
    }

}
