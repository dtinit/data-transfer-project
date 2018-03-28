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
package org.dataportabilityproject.spi.transfer.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.HashMap;
import java.util.Map;
import org.dataportabilityproject.types.transfer.models.DataModel;

/**
 * Temporary data for mail export/import.
 *
 * <p>Exported folder/label ID (or Name) to imported folder/label ID mappings are maintained so that imported messages
 * can be added to the correct folder/label.
 */
@JsonTypeName("org.dataportability:TempMailData")
public class TempMailData extends DataModel {

  @JsonProperty("jobId")
  private final String jobId;

  @JsonProperty("folderMappings")
  private final Map<String, String> folderMappings;

  @JsonCreator
  public TempMailData(
      @JsonProperty("jobId") String jobId,
      @JsonProperty("folderMappings") Map<String, String> folderMappings) {
    this.jobId = jobId;
    this.folderMappings = folderMappings;
  }

  public TempMailData(String jobId) {
    this.jobId = jobId;
    folderMappings = new HashMap<>();
  }

  /** Returns the job id this data is associated with. */
  public String getJobId() {
    return jobId;
  }

  /**
   * Adds a folder/label id (or Name) mapping.
   *
   * @param exportedId the exported folder/label id (or Name)
   * @param importedId the imported folder/label id
   */
  public void addFolderIdMapping(String exportedId, String importedId) {
    folderMappings.put(exportedId, importedId);
  }

  /**
   * Returns the imported folder/label id that is mapped to the exported id (or Name).
   *
   * @param exported the exported id.
   */
  public String getImportedId(String exported) {
    return folderMappings.get(exported);
  }
}
