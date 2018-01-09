package org.dataportabilityproject.spi.datatransfer;

import org.dataportabilityproject.datatransfer.types.models.DataModel;

/**
 * Placeholder.
 */
public interface Exporter<T extends DataModel> {

    T export(Object continuationInformation); // REVIEW: The original throws IOException. Continue to use checked exceptions or use unchecked?
}
