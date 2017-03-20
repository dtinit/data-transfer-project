package org.dataportabilityproject.dataModels;

import java.io.IOException;

public interface Importer<T extends DataModel> {
    void importItem(T object) throws IOException;
}
