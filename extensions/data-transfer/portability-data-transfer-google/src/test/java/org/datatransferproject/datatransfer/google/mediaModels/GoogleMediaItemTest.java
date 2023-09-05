package org.datatransferproject.datatransfer.google.mediaModels;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import org.datatransferproject.types.common.models.photos.PhotoModel;
import org.datatransferproject.types.common.models.videos.VideoModel;
import org.junit.Test;
public class GoogleMediaItemTest {

  @Test
  public void getMimeType_photoModel_mimeTypeFromFilename() throws Exception {
    GoogleMediaItem photoMediaItem = getPhotoMediaItem();
    Map<String, String> filenameToMimeTypeMap = Map.of(
        "file.jpg", "image/jpeg",
        "file.png", "image/png",
        "file.gif", "image/gif",
        "file.webp", "image/webp",
        "file.avif", "image/avif"
    );

    for (Entry entry: filenameToMimeTypeMap.entrySet()) {
      photoMediaItem.setMimeType("INVALID_MIME");
      photoMediaItem.setFilename(entry.getKey().toString());

      PhotoModel photoModel = GoogleMediaItem.convertToPhotoModel(Optional.empty(), photoMediaItem);

      assertEquals(entry.getValue(), photoModel.getMimeType());
    }
  }

  @Test
  public void getMimeType_videoModel_mimeTypeFromFilename() {
    GoogleMediaItem videoMediaItem = getVideoMediaItem();
    Map<String, String> filenameToMimeTypeMap = Map.of(
        "file.flv", "video/x-flv",
        "file.mp4", "video/mp4",
        "file.webm", "video/webm",
        "file.qt", "video/quicktime",
        "file.mov", "video/quicktime"
    );

    for (Entry entry: filenameToMimeTypeMap.entrySet()) {
      videoMediaItem.setMimeType("INVALID_MIME");
      videoMediaItem.setFilename(entry.getKey().toString());

      VideoModel videoModel = GoogleMediaItem.convertToVideoModel(Optional.empty(), videoMediaItem);

      assertEquals(entry.getValue(), videoModel.getMimeType());
    }
  }

  @Test
  public void getMimeType_photoModel_filenameMimeTypeIsNull() {
    GoogleMediaItem photoMediaItem = getPhotoMediaItem();
    photoMediaItem.setFilename("file");
    photoMediaItem.setMimeType("image/webp");

    PhotoModel photoModel = GoogleMediaItem.convertToPhotoModel(Optional.empty(), photoMediaItem);

    assertEquals("image/webp", photoModel.getMimeType());
  }

  @Test
  public void getMimeType_videoModel_filenameMimeTypeIsNull() {
    GoogleMediaItem videoMediaItem = getVideoMediaItem();
    videoMediaItem.setFilename("file");
    videoMediaItem.setMimeType("video/webm");

    VideoModel videoModel = GoogleMediaItem.convertToVideoModel(Optional.empty(), videoMediaItem);

    assertEquals("video/webm", videoModel.getMimeType());
  }

  @Test
  public void getMimeType_photoModel_nullMimeTypeReturnsDefault() {
    GoogleMediaItem photoMediaItem = getPhotoMediaItem();
    photoMediaItem.setFilename("file");
    photoMediaItem.setMimeType(null);

    PhotoModel photoModel = GoogleMediaItem.convertToPhotoModel(Optional.empty(), photoMediaItem);

    // Default defined in GoogleMediaItem
    assertEquals("image/jpg", photoModel.getMimeType());
  }

  @Test
  public void getMimeType_videoModel_nullMimeTypeReturnsDefault() {
    GoogleMediaItem videoMediaItem = getVideoMediaItem();
    videoMediaItem.setFilename("file");
    videoMediaItem.setMimeType(null);

    VideoModel videoModel = GoogleMediaItem.convertToVideoModel(Optional.empty(), videoMediaItem);

    // Default defined in GoogleMediaItem
    assertEquals("video/mp4", videoModel.getMimeType());
  }

  public static GoogleMediaItem getPhotoMediaItem() {
    MediaMetadata photoMetadata = new MediaMetadata();
    photoMetadata.setPhoto(new Photo());

    GoogleMediaItem photoMediaItem = new GoogleMediaItem();
    photoMediaItem.setMimeType("image/png");
    photoMediaItem.setDescription("Description");
    photoMediaItem.setFilename("filename.png");
    photoMediaItem.setBaseUrl("https://www.google.com");
    photoMediaItem.setUploadedTime(new Date());
    photoMediaItem.setId("photo_id");
    photoMediaItem.setMediaMetadata(photoMetadata);
    return photoMediaItem;
  }

  public static GoogleMediaItem getVideoMediaItem() {
    MediaMetadata videoMetadata = new MediaMetadata();
    videoMetadata.setVideo(new Video());

    GoogleMediaItem videoMediaItem = new GoogleMediaItem();
    videoMediaItem.setMimeType("video/mp4");
    videoMediaItem.setDescription("Description");
    videoMediaItem.setFilename("filename.mp4");
    videoMediaItem.setBaseUrl("https://www.google.com");
    videoMediaItem.setUploadedTime(new Date());
    videoMediaItem.setId("video_id");
    videoMediaItem.setMediaMetadata(videoMetadata);
    return videoMediaItem;
  }
}
