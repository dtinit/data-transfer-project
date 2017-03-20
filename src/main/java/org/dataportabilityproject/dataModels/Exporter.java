package org.dataportabilityproject.dataModels;

import java.io.IOException;
import java.util.Collection;

public interface Exporter<T extends DataModel> {
    Collection<T> export() throws IOException;
}
