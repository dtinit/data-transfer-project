package org.datatransferproject.datatransfer.apple.photos;

import static com.google.common.truth.Truth.assertThat;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.http.HttpStatus.SC_OK;
import static org.datatransferproject.types.common.models.media.MediaContainerResource.VIDEOS_COUNT_DATA_NAME;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.datatransferproject.datatransfer.apple.constants.ApplePhotosConstants;
import org.datatransferproject.spi.transfer.provider.ImportResult;
import org.datatransferproject.types.common.models.DataVertical;
import org.datatransferproject.types.common.models.videos.VideoModel;
import org.datatransferproject.types.common.models.videos.VideosContainerResource;
import org.datatransferproject.types.transfer.auth.AppCredentials;
import org.datatransferproject.types.transfer.errors.ErrorDetail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class AppleVideosImporterTest extends AppleImporterTestBase {
  protected AppleVideosImporter appleVideosImporter;

  @BeforeEach
  public void setup() throws Exception {
    super.setup();
    appleVideosImporter =
      new AppleVideosImporter(
        new AppCredentials("key", "secret"), EXPORTING_SERVICE, monitor, factory);
  }

  @Test
  public void importSingleVideo() throws Exception {
    // set up
    final int videoCount = 1;
    final List<VideoModel> videos = createTestVideos(videoCount);
    final Map<String, Integer> dataIdToStatus =
        videos.stream()
            .collect(
                Collectors.toMap(VideoModel::getDataId, VideoModel -> SC_OK));
    setUpGetUploadUrlResponse(dataIdToStatus);
    setUpUploadContentResponse(dataIdToStatus);
    setUpCreateMediaResponse(dataIdToStatus);

    // run test
    VideosContainerResource data = new VideosContainerResource(null, videos);
    final ImportResult importResult =
        appleVideosImporter.importItem(uuid, executor, authData, data);

    // verify correct methods were called
    verify(mediaInterface)
        .getUploadUrl(
            uuid.toString(),
            DataVertical.VIDEOS.getDataType(),
            videos.stream().map(VideoModel::getDataId).collect(Collectors.toList()));
    verify(mediaInterface).uploadContent(anyMap(), anyList());
    verify(mediaInterface).createMedia(anyString(), anyString(), anyList());

    // check the result
    assertThat(importResult.getCounts().isPresent());
    assertThat(importResult.getCounts().get().get(VIDEOS_COUNT_DATA_NAME) == videoCount);
    assertThat(importResult.getBytes().get() == videoCount * VIDEOS_FILE_SIZE);

    final Map<String, Serializable> expectedKnownValue =
        videos.stream()
            .collect(
                Collectors.toMap(
                    VideoModel -> VideoModel.getDataId(),
                    VideoModel -> MEDIA_RECORDID_BASE + VideoModel.getDataId()));
    checkKnownValues(expectedKnownValue);
  }

  @Test
  public void importVideosMultipleBatches() throws Exception {
    // set up
    final int videoCount = ApplePhotosConstants.maxNewMediaRequests + 1;
    ;
    final List<VideoModel> videos = createTestVideos(videoCount);
    final Map<String, Integer> dataIdToStatus =
        videos.stream()
            .collect(
                Collectors.toMap(VideoModel::getDataId, VideoModel -> SC_OK));
    setUpGetUploadUrlResponse(dataIdToStatus);
    setUpUploadContentResponse(dataIdToStatus);
    setUpCreateMediaResponse(dataIdToStatus);

    // run test
    VideosContainerResource data = new VideosContainerResource(null, videos);
    final ImportResult importResult =
        appleVideosImporter.importItem(uuid, executor, authData, data);

    // verify correct methods were called
    verify(mediaInterface, times(2)).getUploadUrl(anyString(), anyString(), anyList());
    verify(mediaInterface)
        .getUploadUrl(
            uuid.toString(),
            DataVertical.VIDEOS.getDataType(),
            videos.subList(0, ApplePhotosConstants.maxNewMediaRequests).stream()
                .map(VideoModel::getDataId)
                .collect(Collectors.toList()));
    verify(mediaInterface)
        .getUploadUrl(
            uuid.toString(),
            DataVertical.VIDEOS.getDataType(),
            videos.subList(ApplePhotosConstants.maxNewMediaRequests, videoCount).stream()
                .map(VideoModel::getDataId)
                .collect(Collectors.toList()));

    verify(mediaInterface, times(2)).uploadContent(anyMap(), anyList());
    verify(mediaInterface, times(2)).createMedia(anyString(), anyString(), anyList());

    // check the result
    assertThat(importResult.getCounts().isPresent());
    assertThat(importResult.getCounts().get().get(VIDEOS_COUNT_DATA_NAME) == videoCount);
    assertThat(importResult.getBytes().get() == videoCount * VIDEOS_FILE_SIZE);
  }

  @Test
  public void importVideosWithFailure() throws Exception {
    // set up
    final int videoCount = ApplePhotosConstants.maxNewMediaRequests + 1;
    ;
    final List<VideoModel> videos = createTestVideos(videoCount);
    final int errorCountGetUploadURL = 10;
    final int errorCountUploadContent = 10;
    final int errorCountCreateMedia = 10;
    final int successCount =
        videoCount - errorCountGetUploadURL - errorCountUploadContent - errorCountCreateMedia;
    final List<String> dataIds =
        videos.stream().map(VideoModel::getDataId).collect(Collectors.toList());
    final Map<String, Integer> datatIdToGetUploadURLStatus =
        setUpErrors(dataIds, 0, errorCountGetUploadURL);
    final Map<String, Integer> datatIdToUploadContentStatus =
        setUpErrors(dataIds, errorCountGetUploadURL, errorCountUploadContent);
    final Map<String, Integer> datatIdToCreateMediaStatus =
        setUpErrors(
            dataIds, errorCountGetUploadURL + errorCountUploadContent, errorCountCreateMedia);
    setUpGetUploadUrlResponse(datatIdToGetUploadURLStatus);
    setUpUploadContentResponse(datatIdToUploadContentStatus);
    setUpCreateMediaResponse(datatIdToCreateMediaStatus);

    // run test
    VideosContainerResource data = new VideosContainerResource(null, videos);
    final ImportResult importResult =
        appleVideosImporter.importItem(uuid, executor, authData, data);

    // verify correct methods were called
    verify(mediaInterface, times(2)).getUploadUrl(anyString(), anyString(), anyList());
    verify(mediaInterface)
        .getUploadUrl(
            uuid.toString(),
            DataVertical.VIDEOS.getDataType(),
            videos.subList(0, ApplePhotosConstants.maxNewMediaRequests).stream()
                .map(VideoModel::getDataId)
                .collect(Collectors.toList()));
    verify(mediaInterface)
        .getUploadUrl(
            uuid.toString(),
            DataVertical.VIDEOS.getDataType(),
            videos.subList(ApplePhotosConstants.maxNewMediaRequests, videoCount).stream()
                .map(VideoModel::getDataId)
                .collect(Collectors.toList()));

    verify(mediaInterface, times(2)).uploadContent(anyMap(), anyList());
    verify(mediaInterface, times(2)).createMedia(anyString(), anyString(), anyList());

    // check the result
    assertThat(importResult.getCounts().isPresent());
    assertThat(importResult.getCounts().get().get(VIDEOS_COUNT_DATA_NAME) == videoCount);
    assertThat(importResult.getBytes().get() == successCount * VIDEOS_FILE_SIZE);

    final Map<String, Serializable> expectedKnownValue =
        videos.stream()
            .filter(
                VideoModel ->
                    datatIdToGetUploadURLStatus.get(VideoModel.getDataId()) == SC_OK)
            .filter(
                VideoModel ->
                    datatIdToUploadContentStatus.get(VideoModel.getDataId()) == SC_OK)
            .filter(
                VideoModel ->
                    datatIdToCreateMediaStatus.get(VideoModel.getDataId()) == SC_OK)
            .collect(
                Collectors.toMap(
                    video -> video.getDataId(),
                    video -> VIDEOS_DATAID_BASE + video.getDataId()));
    checkKnownValues(expectedKnownValue);

    // check errors
    List<ErrorDetail> expectedErrors = new ArrayList<>();
    for (int i = 0;
        i < errorCountGetUploadURL + errorCountUploadContent + errorCountCreateMedia;
        i++) {
      final VideoModel video = videos.get(i);
      final ErrorDetail.Builder errorDetailBuilder =
          ErrorDetail.builder()
              .setId(video.getIdempotentId())
              .setTitle(video.getName())
              .setException(
                  String.format(
                      "java.io.IOException: Fail to get upload url, error code: %d",
                      SC_INTERNAL_SERVER_ERROR));
      if (i < errorCountGetUploadURL) {
        errorDetailBuilder.setException(
            String.format(
                "java.io.IOException: Fail to get upload url, error code: %d",
                SC_INTERNAL_SERVER_ERROR));
      } else if (i < errorCountGetUploadURL + errorCountGetUploadURL) {
        errorDetailBuilder.setException("java.io.IOException: Fail to upload content");
      } else {
        errorDetailBuilder.setException(
            String.format(
                "java.io.IOException: Fail to create media, error code: %d",
                SC_INTERNAL_SERVER_ERROR));
      }
      expectedErrors.add(errorDetailBuilder.build());
    }

    checkErrors(expectedErrors);
    checkRecentErrors(expectedErrors);
  }
}
