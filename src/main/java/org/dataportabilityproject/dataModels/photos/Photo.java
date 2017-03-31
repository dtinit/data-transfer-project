package org.dataportabilityproject.dataModels.photos;

import org.dataportabilityproject.dataModels.DataModel;


public class Photo implements DataModel {
    private final String title;
    private final String fetchableUrl;
    private final String description;
    private final String mediaType;

    public Photo(String title, String fetchableUrl, String description, String mediaType) {
        this.title = title;
        this.fetchableUrl = fetchableUrl;
        this.description = description;
        this.mediaType = mediaType;
    }

    public String getTitle() {
        return title;
    }

    public String getFetchableUrl() {
        return fetchableUrl;
    }

    public String getDescription() {
        return description;
    }

    public String getMediaType() {
        return mediaType;
    }
}
