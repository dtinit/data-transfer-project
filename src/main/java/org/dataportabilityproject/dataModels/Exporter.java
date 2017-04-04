package org.dataportabilityproject.dataModels;

import java.io.IOException;

public interface Exporter<T extends DataModel> {
    T export(ExportInformation continuationInformation) throws IOException;
}
