package org.datatransferproject.datatransfer.generic;

import java.io.IOException;
import org.datatransferproject.types.common.models.ContainerResource;

@FunctionalInterface
public interface ContainerSerializer<C extends ContainerResource, R> {
  public Iterable<ImportableData<R>> apply(C containerResource) throws IOException;
}
