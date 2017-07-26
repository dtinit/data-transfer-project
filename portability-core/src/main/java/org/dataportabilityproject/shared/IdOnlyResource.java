package org.dataportabilityproject.shared;

import com.google.common.base.MoreObjects;
import org.dataportabilityproject.dataModels.Resource;

/**
 * A {@link Resource} containing only a string id.
 */
public class IdOnlyResource implements Resource {
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
}
