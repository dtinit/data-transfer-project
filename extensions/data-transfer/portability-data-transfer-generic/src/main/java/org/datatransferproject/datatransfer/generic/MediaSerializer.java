package org.datatransferproject.datatransfer.generic;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import java.util.Date;
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

class VideoModelExportData extends VideoModel implements MediaSerializer.ExportData {
  private VideoModelExportData(
      String name,
      String contentUrl,
      String description,
      String encodingFormat,
      String dataId,
      String albumId,
      boolean inTempStore,
      Date uploadedTime,
      FavoriteInfo favoriteInfo) {
    super(
        name,
        contentUrl,
        description,
        encodingFormat,
        dataId,
        albumId,
        inTempStore,
        uploadedTime,
        favoriteInfo);
  }

  static VideoModelExportData fromModel(VideoModel model) {
    return new VideoModelExportData(
        model.getName(),
        model.getContentUrl().toString(),
        model.getDescription(),
        model.getEncodingFormat(),
        model.getDataId(),
        model.getAlbumId(),
        model.isInTempStore(),
        model.getUploadedTime(),
        model.getFavoriteInfo());
  }
}

class PhotoModelExportData extends PhotoModel implements MediaSerializer.ExportData {
  private PhotoModelExportData(
      String title,
      String fetchableUrl,
      String description,
      String mediaType,
      String dataId,
      String albumId,
      boolean inTempStore,
      String sha1,
      Date uploadedTime,
      FavoriteInfo favoriteInfo) {
    super(
        title,
        fetchableUrl,
        description,
        mediaType,
        dataId,
        albumId,
        inTempStore,
        sha1,
        uploadedTime,
        favoriteInfo);
  }

  static PhotoModelExportData fromModel(PhotoModel model) {
    return new PhotoModelExportData(
        model.getTitle(),
        model.getFetchableUrl().toString(),
        model.getDescription(),
        model.getMediaType(),
        model.getDataId(),
        model.getAlbumId(),
        model.isInTempStore(),
        model.getSha1(),
        model.getUploadedTime(),
        model.getFavoriteInfo());
  }
}

public class MediaSerializer {
  static final String SCHEMA_SOURCE_ALBUM =
      GenericTransferConstants.SCHEMA_SOURCE_BASE
          + "/portability-types-common/src/main/java/org/datatransferproject/types/common/models/media/MediaAlbum.java";
  static final String SCHEMA_SOURCE_VIDEO =
      GenericTransferConstants.SCHEMA_SOURCE_BASE
          + "/portability-types-common/src/main/java/org/datatransferproject/types/common/models/videos/VideoModel.java";
  static final String SCHEMA_SOURCE_PHOTO =
      GenericTransferConstants.SCHEMA_SOURCE_BASE
          + "/portability-types-common/src/main/java/org/datatransferproject/types/common/models/photos/PhotoModel.java";

  @JsonSubTypes({
    @JsonSubTypes.Type(value = MediaAlbumExportData.class, name = "MediaAlbum"),
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
                                  VideoModelExportData.fromModel(video), SCHEMA_SOURCE_VIDEO),
                              video.getIdempotentId(),
                              video.getName());
                        }),
                container.getPhotos().stream()
                    .map(
                        photo -> {
                          return new ImportableFileData<>(
                              photo,
                              new GenericPayload<ExportData>(
                                  PhotoModelExportData.fromModel(photo), SCHEMA_SOURCE_PHOTO),
                              photo.getIdempotentId(),
                              photo.getName());
                        })))
        .collect(Collectors.toList());
  }
}
