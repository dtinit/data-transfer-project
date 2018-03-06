package org.dataportabilityproject.spi.transfer.types;

import com.google.common.base.MoreObjects;

public class IdOnlyResource {
  private final String id;

  public IdOnlyResource(String id) {
    this.id = id;
  }

  public String getId() {
    return id;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
            .add("id", id)
            .toString();
  }

  @Override
  public boolean equals(Object object) {
    if (object == null) {
      return false;
    }
    if (!IdOnlyResource.class.isAssignableFrom(object.getClass())) {
      return false;
    }
    IdOnlyResource resource = (IdOnlyResource) object;
    return this.id.equals(resource.getId());
  }
}
