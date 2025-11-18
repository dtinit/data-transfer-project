package org.datatransferproject.datatransfer.generic;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.stream.Collectors;
import org.datatransferproject.types.common.models.social.SocialActivityActor;
import org.datatransferproject.types.common.models.social.SocialActivityContainerResource;
import org.datatransferproject.types.common.models.social.SocialActivityModel;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
class SocialActivityMetadata {
  private final SocialActivityActor actor;

  @JsonCreator
  public SocialActivityMetadata(@JsonProperty SocialActivityActor actor) {
    this.actor = actor;
  }

  public SocialActivityActor getActor() {
    return actor;
  }
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
class SocialActivity implements SocialPostsSerializer.ExportData {
  private final SocialActivityMetadata metadata;
  private final SocialActivityModel activity;

  public SocialActivity(SocialActivityMetadata metadata, SocialActivityModel activity) {
    this.metadata = metadata;
    this.activity = activity;
  }

  public SocialActivityMetadata getMetadata() {
    return metadata;
  }

  public SocialActivityModel getActivity() {
    return activity;
  }
}

public class SocialPostsSerializer {

  @JsonSubTypes({
    @JsonSubTypes.Type(value = SocialActivity.class),
  })
  public interface ExportData {}

  static final String SCHEMA_SOURCE =
      GenericTransferConstants.SCHEMA_SOURCE_BASE
          + "/extensions/data-transfer/portability-data-transfer-generic/src/main/java/org/datatransferproject/datatransfer/generic/SocialPostsSerializer.java";

  public static Iterable<ImportableData<ExportData>> serialize(
      SocialActivityContainerResource container) {
    return container.getActivities().stream()
        .map(
            activity ->
                new ImportableData<>(
                    new GenericPayload<ExportData>(
                        // "actor" is stored at the container level, but isn't repliacted
                        // in the tree of activity, so merge it in a metadata field
                        new SocialActivity(
                            new SocialActivityMetadata(container.getActor()), activity),
                        SCHEMA_SOURCE),
                    activity.getIdempotentId(),
                    activity.getName()))
        .collect(Collectors.toList());
  }
}
