package org.datatransferproject.spi.transfer.store;

import org.datatransferproject.types.common.PortableType;

/**
 * Used by {@link org.datatransferproject.spi.transfer.provider.Importer}s and {@link
 * org.datatransferproject.spi.transfer.provider.Exporter}s to save scratch data needed during
 * the transfer process.
 */
public interface TransferStore {

    /**
     * Creates a data entry.
     *
     * @param entity the entry
     */
    void create(PortableType entity);

    /**
     * Updates an existing entry.
     *
     * @param entity the entry
     */
    void update(PortableType entity);

    /**
     * Returns an existing entry or null if not found.
     *
     * @param id the entry id
     */
    PortableType find(String id);

    /**
     * Removes and entry
     *
     * @param id the entry id
     */
    void remove(String id);
}
