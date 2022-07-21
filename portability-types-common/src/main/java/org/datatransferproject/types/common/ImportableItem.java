package org.datatransferproject.types.common;

import com.fasterxml.jackson.annotation.JsonIgnore;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface ImportableItem {

  @JsonIgnore
  @Nonnull
  String getIdempotentId();

  @JsonIgnore
  @Nullable
  default String getName() {
    return null;
  }
}
