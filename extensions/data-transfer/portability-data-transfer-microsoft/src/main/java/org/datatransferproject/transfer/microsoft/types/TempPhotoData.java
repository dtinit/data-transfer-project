package org.datatransferproject.transfer.microsoft.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.datatransferproject.types.common.models.DataModel;

import java.util.HashMap;
import java.util.Map;

/**
 * Temporary data for photo export/import.
 *
 * <p>Exported album ID to imported album ID mappings are maintained so that imported photos can be
 * added to the correct album.
 */
@JsonTypeName("org.dataportability:TempCalendarData")
public class TempPhotoData extends DataModel {

  @JsonProperty("jobId")
  private final String jobId;

  private final Map<String, String> albumMappings;

  @JsonCreator
  public TempPhotoData(
      @JsonProperty("jobId") String jobId,
      @JsonProperty("albumMappings") Map<String, String> mappings) {
    this.jobId = jobId;
    this.albumMappings = mappings;
  }

  public TempPhotoData(String jobId) {
    this.jobId = jobId;
    albumMappings = new HashMap<>();
  }

  /** Returns the job id this data is associated with. */
  public String getJobId() {
    return jobId;
  }

  /**
   * Adds a album id mapping.
   *
   * @param exportedId the exported album id
   * @param importedId the imported album id
   */
  public void addIdMapping(String exportedId, String importedId) {
    albumMappings.put(exportedId, importedId);
  }

  /**
   * Returns the imported album id that is mapped to the exported id.
   *
   * @param exported the exported id.
   */
  public String getImportedId(String exported) {
    return albumMappings.get(exported);
  }
}
