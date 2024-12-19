package org.datatransferproject.datatransfer.generic;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.datatransferproject.types.common.models.FavoriteInfo;
import org.datatransferproject.types.common.models.media.MediaAlbum;
import org.datatransferproject.types.common.models.media.MediaContainerResource;
import org.datatransferproject.types.common.models.photos.PhotoModel;
import org.datatransferproject.types.common.models.videos.VideoModel;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
@JsonTypeName("Album")
class AlbumExportData implements MediaSerializer.ExportData {
  @JsonProperty private final String id;
  @JsonProperty private final String name;
  @JsonProperty private final String description;

  private AlbumExportData(String id, String name, String description) {
    this.id = id;
    this.name = name;
    this.description = description;
  }

  static AlbumExportData fromModel(MediaAlbum model) {
    return new AlbumExportData(model.getId(), model.getName(), model.getDescription());
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
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
@JsonTypeName("FavoriteInfo")
class FavoriteInfoExportData {
  @JsonProperty private final boolean favorite;
  @JsonProperty private final ZonedDateTime lastUpdateTime;

  private FavoriteInfoExportData(boolean favorite, ZonedDateTime lastUpdateTime) {
    this.favorite = favorite;
    this.lastUpdateTime = lastUpdateTime;
  }

  public static FavoriteInfoExportData fromModel(FavoriteInfo model) {
    return new FavoriteInfoExportData(
        model.getFavorited(),
        ZonedDateTime.ofInstant(model.getLastUpdateTime().toInstant(), ZoneOffset.UTC));
  }

  public boolean isFavorite() {
    return favorite;
  }

  public ZonedDateTime getLastUpdateTime() {
    return lastUpdateTime;
  }
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
class MediaItemExportData implements MediaSerializer.ExportData {
  @JsonProperty private final String name;
  @JsonProperty private final String description;
  @JsonProperty private final String albumId;
  @JsonProperty private final ZonedDateTime uploadedTime;
  @JsonProperty private final FavoriteInfoExportData favoriteInfo;

  public MediaItemExportData(
      String name,
      String description,
      String albumId,
      ZonedDateTime uploadedTime,
      FavoriteInfoExportData favoriteInfo) {
    this.name = name;
    this.description = description;
    this.albumId = albumId;
    this.uploadedTime = uploadedTime;
    this.favoriteInfo = favoriteInfo;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public String getAlbumId() {
    return albumId;
  }

  public ZonedDateTime getUploadedTime() {
    return uploadedTime;
  }

  public FavoriteInfoExportData getFavoriteInfo() {
    return favoriteInfo;
  }
}

@JsonTypeName("Video")
class VideoExportData extends MediaItemExportData {
  private VideoExportData(
      String name,
      String description,
      String albumId,
      ZonedDateTime uploadedTime,
      FavoriteInfoExportData favoriteInfo) {
    super(name, description, albumId, uploadedTime, favoriteInfo);
  }

  static VideoExportData fromModel(VideoModel model) {
    return new VideoExportData(
        model.getName(),
        model.getDescription(),
        model.getAlbumId(),
        ZonedDateTime.ofInstant(model.getUploadedTime().toInstant(), ZoneOffset.UTC),
        FavoriteInfoExportData.fromModel(model.getFavoriteInfo()));
  }
}

@JsonTypeName("Photo")
class PhotoExportData extends MediaItemExportData {
  private PhotoExportData(
      String name,
      String description,
      String albumId,
      ZonedDateTime uploadedTime,
      FavoriteInfoExportData favoriteInfo) {
    super(name, description, albumId, uploadedTime, favoriteInfo);
  }

  static PhotoExportData fromModel(PhotoModel model) {
    return new PhotoExportData(
        model.getName(),
        model.getDescription(),
        model.getAlbumId(),
        ZonedDateTime.ofInstant(model.getUploadedTime().toInstant(), ZoneOffset.UTC),
        FavoriteInfoExportData.fromModel(model.getFavoriteInfo()));
  }
}

public class MediaSerializer {
  static final String SCHEMA_SOURCE =
      GenericTransferConstants.SCHEMA_SOURCE_BASE
          + "/extensions/data-transfer/portability-data-transfer-generic/src/main/java/org/datatransferproject/datatransfer/generic/MediaSerializer.java";

  @JsonSubTypes({
    @JsonSubTypes.Type(AlbumExportData.class),
    @JsonSubTypes.Type(VideoExportData.class),
    @JsonSubTypes.Type(PhotoExportData.class),
  })
  public interface ExportData {}

  public static Iterable<ImportableData<ExportData>> serialize(MediaContainerResource container) {
    return Stream.concat(
            container.getAlbums().stream()
                .map(
                    album ->
                        new ImportableData<>(
                            new GenericPayload<ExportData>(
                                AlbumExportData.fromModel(album), SCHEMA_SOURCE),
                            album.getIdempotentId(),
                            album.getName())),
            Stream.concat(
                container.getVideos().stream()
                    .map(
                        (video) -> {
                          return new ImportableFileData<>(
                              video,
                              video.getMimeType(),
                              new GenericPayload<ExportData>(
                                  VideoExportData.fromModel(video), SCHEMA_SOURCE),
                              video.getIdempotentId(),
                              video.getName());
                        }),
                container.getPhotos().stream()
                    .map(
                        photo -> {
                          return new ImportableFileData<>(
                              photo,
                              photo.getMimeType(),
                              new GenericPayload<ExportData>(
                                  PhotoExportData.fromModel(photo), SCHEMA_SOURCE),
                              photo.getIdempotentId(),
                              photo.getName());
                        })))
        .collect(Collectors.toList());
  }
}
