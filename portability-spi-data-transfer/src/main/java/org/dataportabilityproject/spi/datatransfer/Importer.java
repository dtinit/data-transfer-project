package org.dataportabilityproject.spi.datatransfer;

import org.dataportabilityproject.datatransfer.types.models.DataModel;

/**
 * Imports data into a destination service.
 */
public interface Importer<T extends DataModel> {

    /**
     * Returns the service provider id of the importer.
     */
    String getServiceProviderId();

    /**
     * Returns supported content types.
     */
    String[] getContentTypes();

    /**
     * Imports data.
     *
     * @param data the data
     */
    void importItem(T data); // REVIEW: The original throws IOException. Continue to use checked exceptions or use unchecked?

}
