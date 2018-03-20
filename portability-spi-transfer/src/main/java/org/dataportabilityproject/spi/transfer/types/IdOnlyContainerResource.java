package org.dataportabilityproject.spi.transfer.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.dataportabilityproject.types.transfer.models.ContainerResource;

/** A resource container referenced by id. */
@JsonTypeName("org.dataportability:IdOnlyContainerResource")
public class IdOnlyContainerResource extends ContainerResource {
  private final String id;

  /**
   * Ctor.
   *
   * @param id the container id.
   */
  @JsonCreator
  public IdOnlyContainerResource(@JsonProperty("id") String id) {
    this.id = id;
  }

  public String getId() {
    return id;
  }

  @Override
  public boolean equals(Object object) {
    if (object == null) {
      return false;
    }
    if (!IdOnlyContainerResource.class.isAssignableFrom(object.getClass())) {
      return false;
    }
    IdOnlyContainerResource idResource= (IdOnlyContainerResource) object;
    return this.id.equals(idResource.getId());
  }
}
