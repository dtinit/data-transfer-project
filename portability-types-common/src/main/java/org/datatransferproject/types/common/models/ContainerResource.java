package org.datatransferproject.types.common.models;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.datatransferproject.types.common.models.calendar.CalendarContainerResource;
import org.datatransferproject.types.common.models.mail.MailContainerResource;
import org.datatransferproject.types.common.models.photos.PhotosContainerResource;
import org.datatransferproject.types.common.models.tasks.TaskContainerResource;

/**
 * A resource that contains data items such as a photo album or song list.
 *
 * <p>Concrete subtypes must use {@link com.fasterxml.jackson.annotation.JsonTypeName} to specify a
 * type discriminator used for deserialization.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
@JsonSubTypes({
        @JsonSubTypes.Type(PhotosContainerResource.class),
        @JsonSubTypes.Type(MailContainerResource.class),
        @JsonSubTypes.Type(CalendarContainerResource.class),
        @JsonSubTypes.Type(TaskContainerResource.class),
        @JsonSubTypes.Type(IdOnlyContainerResource.class)
})
public abstract class ContainerResource extends DataModel {}
