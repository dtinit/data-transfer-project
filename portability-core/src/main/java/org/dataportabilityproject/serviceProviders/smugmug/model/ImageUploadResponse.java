package org.dataportabilityproject.serviceProviders.smugmug.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public final class ImageUploadResponse {
  @JsonProperty("stat")
  private String stat;

  @JsonProperty("method")
  private String method;

  @JsonProperty("image")
  private ImageInfo image;

  public String getStat() {
    return stat;
  }

  public ImageInfo getImage() {
    return image;
  }

  public static class ImageInfo {
    @JsonProperty("ImageUri")
    private String imageUri;

    @JsonProperty("AlbumImageUri")
    private String albumImageUri;

    @JsonProperty("StatusImageReplaceUri")
    private String statusImageReplaceUri;

    @JsonProperty("URL")
    private String url;

    public String getImageUri() {
      return imageUri;
    }

    public String getAlbumImageUri() {
      return albumImageUri;
    }

    public String getUrl() {
      return url;
    }
  }
}
