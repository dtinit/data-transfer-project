package org.datatransferproject.datatransfer.generic;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.datatransferproject.types.common.models.FavoriteInfo;
import org.datatransferproject.types.common.models.media.MediaAlbum;
import org.datatransferproject.types.common.models.media.MediaContainerResource;
import org.datatransferproject.types.common.models.photos.PhotoModel;
import org.datatransferproject.types.common.models.videos.VideoModel;

class MediaAlbumExportData extends MediaAlbum implements MediaSerializer.ExportData {
  private MediaAlbumExportData(String id, String name, String description) {
    super(id, name, description);
  }

  static MediaAlbumExportData fromModel(MediaAlbum model) {
    return new MediaAlbumExportData(model.getId(), model.getName(), model.getDescription());
  }
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
class MediaItemExportData implements MediaSerializer.ExportData {
  private String name;
  private String description;
  private String albumId;
  private ZonedDateTime uploadedTime;
  // TODO: consider redeclaring FavouriteInfo with ZonedDateTime to fix the inconsistency with date
  // serialization
  private FavoriteInfo favoriteInfo;

  public MediaItemExportData(
      @JsonProperty String name,
      @JsonProperty String description,
      @JsonProperty String albumId,
      @JsonProperty ZonedDateTime uploadedTime,
      @JsonProperty FavoriteInfo favoriteInfo) {
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

  public FavoriteInfo getFavoriteInfo() {
    return favoriteInfo;
  }
}

class VideoModelExportData extends MediaItemExportData {
  public VideoModelExportData(
      String name,
      String description,
      String albumId,
      ZonedDateTime uploadedTime,
      FavoriteInfo favoriteInfo) {
    super(name, description, albumId, uploadedTime, favoriteInfo);
  }

  static VideoModelExportData fromModel(VideoModel model) {
    return new VideoModelExportData(
        model.getName(),
        model.getDescription(),
        model.getAlbumId(),
        ZonedDateTime.ofInstant(model.getUploadedTime().toInstant(), ZoneOffset.UTC),
        model.getFavoriteInfo());
  }
}

class PhotoModelExportData extends MediaItemExportData {
  public PhotoModelExportData(
      String name,
      String description,
      String albumId,
      ZonedDateTime uploadedTime,
      FavoriteInfo favoriteInfo) {
    super(name, description, albumId, uploadedTime, favoriteInfo);
  }

  static PhotoModelExportData fromModel(PhotoModel model) {
    return new PhotoModelExportData(
        model.getName(),
        model.getDescription(),
        model.getAlbumId(),
        ZonedDateTime.ofInstant(model.getUploadedTime().toInstant(), ZoneOffset.UTC),
        model.getFavoriteInfo());
  }
}

public class MediaSerializer {
  static final String SCHEMA_SOURCE_ALBUM =
      GenericTransferConstants.SCHEMA_SOURCE_BASE
          + "/portability-types-common/src/main/java/org/datatransferproject/types/common/models/media/MediaAlbum.java";
  static final String SCHEMA_SOURCE_MEDIA =
      GenericTransferConstants.SCHEMA_SOURCE_BASE
          + "/extensions/data-transfer/portability-data-transfer-generic/src/main/java/org/datatransferproject/datatransfer/generic/MediaSerializer.java";

  @JsonSubTypes({
    @JsonSubTypes.Type(value = MediaAlbumExportData.class, name = "MediaAlbum"),
    // TODO: consider naming
    @JsonSubTypes.Type(value = VideoModelExportData.class, name = "VideoModel"),
    @JsonSubTypes.Type(value = PhotoModelExportData.class, name = "PhotoModel"),
  })
  public interface ExportData {}

  public static Iterable<ImportableData<ExportData>> serialize(MediaContainerResource container) {
    return Stream.concat(
            container.getAlbums().stream()
                .map(
                    album ->
                        new ImportableData<>(
                            new GenericPayload<ExportData>(
                                MediaAlbumExportData.fromModel(album), SCHEMA_SOURCE_ALBUM),
                            album.getIdempotentId(),
                            album.getName())),
            Stream.concat(
                container.getVideos().stream()
                    .map(
                        (video) -> {
                          return new ImportableFileData<>(
                              video,
                              new GenericPayload<ExportData>(
                                  VideoModelExportData.fromModel(video), SCHEMA_SOURCE_MEDIA),
                              video.getIdempotentId(),
                              video.getName());
                        }),
                container.getPhotos().stream()
                    .map(
                        photo -> {
                          return new ImportableFileData<>(
                              photo,
                              new GenericPayload<ExportData>(
                                  PhotoModelExportData.fromModel(photo), SCHEMA_SOURCE_MEDIA),
                              photo.getIdempotentId(),
                              photo.getName());
                        })))
        .collect(Collectors.toList());
  }
}
