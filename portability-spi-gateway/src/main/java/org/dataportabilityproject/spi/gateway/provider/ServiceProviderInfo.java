package org.dataportabilityproject.spi.gateway.provider;

import java.util.Objects;

/**
 * Describes a registered service provider in the system.
 */
public class ServiceProviderInfo {
    private final String id;
    private final String name;
    private final String description;

    private String[] transferDataTypes;

    public ServiceProviderInfo(String id, String name, String description, String... transferDataTypes) {
        Objects.requireNonNull(id, "Id must not be null");
        Objects.requireNonNull(name, "Name must not be null");
        Objects.requireNonNull(name, "Description must not be null");
        Objects.requireNonNull(transferDataTypes, "Transfer types must not be null");
        this.id = id;
        this.name = name;
        this.description = description;
        this.transferDataTypes = transferDataTypes;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String[] getTransferDataTypes() {
        return transferDataTypes;
    }

}
