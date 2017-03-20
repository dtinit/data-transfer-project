package org.dataportabilityproject.serviceProviders.rememberTheMilk.model;

import com.google.api.client.util.Joiner;
import com.google.api.client.util.Key;

import java.util.List;

/**
 * A tasks series, see: https://www.rememberthemilk.com/services/api/tasks.rtm
 */
public class TaskSeries {
    @Key("@id")
    public int id;

    @Key("@created")
    public String created;

    @Key("@modified")
    public String modified;

    @Key("@name")
    public String name;

    @Key("@source")
    public String source;

    @Key("@url")
    public String url;

    @Key("@location_id")
    public String location_id;

    @Key("tags")
    public String tags;

    @Key("participants")
    public String participants;

    @Key("notes")
    public Notes notes;

    @Key("task")
    public List<Task> tasks;

    @Override
    public String toString() {
        return String.format("TaskSeries(id=%d created=%s modified=%s name=%s source=%s url=%s, notes=%s tasks:%s)",
                id, created, modified, name, source, url, notes,
                (tasks == null || tasks.isEmpty()) ? "" : Joiner.on('\n').join(tasks));
    }
}
