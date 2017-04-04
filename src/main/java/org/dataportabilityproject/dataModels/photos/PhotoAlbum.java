package org.dataportabilityproject.dataModels.photos;


import java.io.Serializable;

public class PhotoAlbum implements Serializable {
    private final String id;
    private final String name;
    private final String description;

    /** The {@code id} is used to associate photos with this album. **/
    public PhotoAlbum(String id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getId() {
        return id;
    }
}
