package org.dataportabilityproject.datatransfer.types;

/**
 * A uniquely identifiable entity in the system.
 */
public abstract class EntityType extends PortableType {
    private String id;

    /**
     * Returns the unique identifier.
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the unique identifier.
     */
    public void setId(String id) {
        this.id = id;
    }
}
