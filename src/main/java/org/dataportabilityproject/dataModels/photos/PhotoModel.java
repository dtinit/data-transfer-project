package org.dataportabilityproject.dataModels.photos;

public class PhotoModel {
    private final String title;
    private final String fetchableUrl;
    private final String description;
    private final String mediaType;
    private final String albumId;

    public PhotoModel(
        String title,
        String fetchableUrl,
        String description,
        String mediaType,
        String albumId) {
        this.title = title;
        this.fetchableUrl = fetchableUrl;
        this.description = description;
        this.mediaType = mediaType;
        this.albumId = albumId;
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

    public String getAlbumId() {
        return albumId;
    }
}
