/*
 * Copyright 2018 The Data-Portability Project Authors.
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
package org.dataportabilityproject.transfer.microsoft.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.dataportabilityproject.types.transfer.models.DataModel;

import java.util.HashMap;
import java.util.Map;

/**
 * Temporary data for calendar export/import.
 *
 * Exported calendar ID to imported calendar ID mappings are maintained so that imported events can be added to the correct calendar.
 */
@JsonTypeName("org.dataportability:TempCalendarData")
public class TempCalendarData extends DataModel {

    @JsonProperty("jobId")
    private final String jobId;

    @JsonProperty("calendarMappings")
    private final Map<String, String> calendarMappings;

    @JsonCreator
    public TempCalendarData(@JsonProperty("jobId") String jobId, @JsonProperty("calendarMappings") Map<String, String> calendarMappings) {
        this.jobId = jobId;
        this.calendarMappings = calendarMappings;
    }

    public TempCalendarData(String jobId) {
        this.jobId = jobId;
        calendarMappings = new HashMap<>();
    }

    /**
     * Returns the job id this data is associated with.
     */
    public String getJobId() {
        return jobId;
    }

    /**
     * Adds a calendar id mapping.
     *
     * @param exportedId the exported calendar id
     * @param importedId the imported calendar id
     */
    public void addIdMapping(String exportedId, String importedId) {
        calendarMappings.put(exportedId, importedId);
    }

    /**
     * Returns the imported calendar id that is mapped to the exported id.
     *
     * @param exported the exported id.
     */
    public String getImportedId(String exported) {
        return calendarMappings.get(exported);
    }
}
