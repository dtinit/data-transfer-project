package org.dataportabilityproject.dataModels;

import org.dataportabilityproject.shared.PortableDataType;

/**
 * Empty interface to signify an item is a Data Model.
 */
public interface DataModel {
    PortableDataType getDataType();
}
