package org.dataportabilityproject.dataModels.photos;


import java.util.Collection;
import org.dataportabilityproject.dataModels.DataModel;

public class PhotoAlbum implements DataModel {
    private final String name;
    private final String description;
    private final Collection<Photo> photos;

    public PhotoAlbum(String name, String description, Collection<Photo> photos) {
        this.name = name;
        this.description = description;
        this.photos = photos;
    }

    public String getName() {
        return name;
    }

    public Collection<Photo> getPhotos() {
        return photos;
    }

    public String getDescription() {
        return description;
    }
}
