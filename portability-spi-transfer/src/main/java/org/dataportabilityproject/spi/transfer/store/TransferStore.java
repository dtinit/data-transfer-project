package org.dataportabilityproject.spi.transfer.store;

import org.dataportabilityproject.types.transfer.EntityType;

/**
 * Used by {@link org.dataportabilityproject.spi.transfer.provider.Importer}s and {@link org.dataportabilityproject.spi.transfer.provider.Exporter}s to save scratch
 * data needed during the transfer process.
 */
public interface TransferStore {

    /**
     * Creates a data entry.
     *
     * @param entity the entry
     */
    void create(EntityType entity);

    /**
     * Updates an existing entry.
     *
     * @param entity the entry
     */
    void update(EntityType entity);

    /**
     * Returns an existing entry or null if not found.
     *
     * @param id the entry id
     */
    EntityType find(String id);

    /**
     * Removes and entry
     *
     * @param id the entry id
     */
    void remove(String id);
}
