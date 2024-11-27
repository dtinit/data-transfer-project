package org.datatransferproject.datatransfer.generic;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.datatransferproject.types.common.models.media.MediaContainerResource;

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

  public static Iterable<ImportableData> serialize(
      MediaContainerResource container, ObjectMapper objectMapper) {
    return Stream.concat(
            container.getAlbums().stream()
                .map(
                    album ->
                        new ImportableData(
                            objectMapper.valueToTree(
                                new GenericPayload<>(album, SCHEMA_SOURCE_ALBUM)),
                            album.getIdempotentId(),
                            album.getName())),
            Stream.concat(
                container.getVideos().stream()
                    .map(
                        (video) -> {
                          return new ImportableFileData(
                              video,
                              objectMapper.valueToTree(
                                  new GenericPayload<>(video, SCHEMA_SOURCE_VIDEO)),
                              video.getIdempotentId(),
                              video.getName());
                        }),
                container.getPhotos().stream()
                    .map(
                        photo -> {
                          return new ImportableFileData(
                              photo,
                              objectMapper.valueToTree(
                                  new GenericPayload<>(photo, SCHEMA_SOURCE_PHOTO)),
                              photo.getIdempotentId(),
                              photo.getName());
                        })))
        .collect(Collectors.toList());
  }
}
