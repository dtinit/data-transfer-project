package org.datatransferproject.datatransfer.google.mediaModels;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import static java.lang.String.format;
import static org.junit.Assert.assertTrue;
import java.text.ParseException;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import org.datatransferproject.types.common.models.photos.PhotoModel;
import org.datatransferproject.types.common.models.videos.VideoModel;
import org.junit.Test;

public class GoogleMediaItemTest {
  private static final ObjectMapper mapper = new ObjectMapper()
      .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);

  @Test
  public void googleMediaItem_isSerializable() {
    String photoStringJSON = "{\"cameraMake\":\"testMake\", \"cameraModel\":\"testModel\","
        + "\"focalLength\":\"5.0\", \"apertureFNumber\":\"2.0\", \"isoEquivalent\":\"8.0\", "
        + "\"exposureTime\":\"testExposureTime\"}";
    String videoStringJSON = "{\"cameraMake\":\"testMake\", \"cameraModel\":\"testModel\","
        + "\"fps\": \"30\", \"status\": \"READY\"}";
    String mediaMetadataStringJSON = format("{\"photo\": %s, \"video\": %s}", photoStringJSON, videoStringJSON);
    String googleMediaItemStringJSON = format("{\"id\":\"test_id\", \"description\":\"test description\","
        + " \"baseUrl\":\"www.testUrl.com\", \"mimeType\":\"image/png\", \"mediaMetadata\": %s,"
        + " \"filename\":\"filename.png\", \"productUrl\":\"www.testProductUrl.com\", "
        + "\"uploadedTime\":\"1697153355456\"}", mediaMetadataStringJSON);

    boolean serializable = true;
    // Turning an object into a byte array can only be done if the class is serializable.
    try {
      GoogleMediaItem googleMediaItem = mapper.readValue(googleMediaItemStringJSON, GoogleMediaItem.class);
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      ObjectOutputStream oos = new ObjectOutputStream(bos);
      oos.writeObject(googleMediaItem);
      oos.flush();
      byte [] data = bos.toByteArray();
    } catch (Exception e) {
      serializable = false;
    }
    assertTrue(serializable);
  }

  @Test
  public void getMimeType_photoModel_mimeTypeFromFilename() throws ParseException {
    GoogleMediaItem photoMediaItem = getPhotoMediaItem();
    Map<String, String> filenameToMimeTypeMap = Map.of(
        "file.jpg", "image/jpeg",
        "file.png", "image/png",
        "file.gif", "image/gif",
        "file.webp", "image/webp"
    );

    for (Entry entry: filenameToMimeTypeMap.entrySet()) {
      photoMediaItem.setMimeType("INVALID_MIME");
      photoMediaItem.setFilename(entry.getKey().toString());

      PhotoModel photoModel = GoogleMediaItem.convertToPhotoModel(Optional.empty(), photoMediaItem);

      assertEquals(entry.getValue(), photoModel.getMimeType());
    }
  }

  @Test
  public void getMimeType_videoModel_mimeTypeFromFilename() throws ParseException{
    GoogleMediaItem videoMediaItem = getVideoMediaItem();
    Map<String, String> filenameToMimeTypeMap =
        Map.of(
            "file.flv", "video/x-flv",
            "file.mp4", "video/mp4",
            "file.webm", "video/webm",
            "file.qt", "video/quicktime",
            "file.mov", "video/quicktime",
            "file.mkv", "video/x-matroska",
            "file.wmv", "video/x-ms-wmv",
            "file.3gp", "video/3gpp");

    for (Entry entry: filenameToMimeTypeMap.entrySet()) {
      videoMediaItem.setMimeType("INVALID_MIME");
      videoMediaItem.setFilename(entry.getKey().toString());

      VideoModel videoModel = GoogleMediaItem.convertToVideoModel(Optional.empty(),
          videoMediaItem);

      assertEquals(entry.getValue(), videoModel.getMimeType());
    }
  }

  @Test
  public void getMimeType_photoModel_filenameMimeTypeIsNull() throws ParseException{
    GoogleMediaItem photoMediaItem = getPhotoMediaItem();
    photoMediaItem.setFilename("file");
    photoMediaItem.setMimeType("image/webp");

    PhotoModel photoModel = GoogleMediaItem.convertToPhotoModel(Optional.empty(), photoMediaItem);

    assertEquals("image/webp", photoModel.getMimeType());
  }

  @Test
  public void getMimeType_videoModel_filenameMimeTypeIsNull() throws ParseException{
    GoogleMediaItem videoMediaItem = getVideoMediaItem();
    videoMediaItem.setFilename("file");
    videoMediaItem.setMimeType("video/webm");

    VideoModel videoModel = GoogleMediaItem.convertToVideoModel(Optional.empty(), videoMediaItem);

    assertEquals("video/webm", videoModel.getMimeType());
  }

  @Test
  public void getMimeType_photoModel_nullMimeTypeReturnsDefault() throws ParseException{
    GoogleMediaItem photoMediaItem = getPhotoMediaItem();
    photoMediaItem.setFilename("file");
    photoMediaItem.setMimeType(null);

    PhotoModel photoModel = GoogleMediaItem.convertToPhotoModel(Optional.empty(), photoMediaItem);

    // Default defined in GoogleMediaItem
    assertEquals("image/jpg", photoModel.getMimeType());
  }

  @Test
  public void getMimeType_videoModel_nullMimeTypeReturnsDefault() throws ParseException{
    GoogleMediaItem videoMediaItem = getVideoMediaItem();
    videoMediaItem.setFilename("file");
    videoMediaItem.setMimeType(null);

    VideoModel videoModel = GoogleMediaItem.convertToVideoModel(Optional.empty(), videoMediaItem);

    // Default defined in GoogleMediaItem
    assertEquals("video/mp4", videoModel.getMimeType());
  }

  @Test
  public void getMimeType_photoModel_unsupportedFileExtension() throws ParseException{
    GoogleMediaItem photoMediaItem = getPhotoMediaItem();
    photoMediaItem.setFilename("file.avif");
    photoMediaItem.setMimeType("image/png");

    PhotoModel photoModel = GoogleMediaItem.convertToPhotoModel(Optional.empty(), photoMediaItem);

    // defaults to the mimetype that is already set, as avif is not handled.
    assertEquals("image/png", photoModel.getMimeType());
  }

  @Test
  public void getUploadTime_videoModel() throws ParseException {
    String fakePhotosApiTimestamp = "2023-10-02T22:33:38Z";
    GoogleMediaItem videoMediaItem = getVideoMediaItem();
    MediaMetadata metadata = new MediaMetadata();
    metadata.setVideo(new Video());
    // CreationTime in GoogleMediaItem is populated as uploadTime in our common models.
    metadata.setCreationTime(fakePhotosApiTimestamp);
    videoMediaItem.setMediaMetadata(metadata);

    VideoModel videoModel = GoogleMediaItem.convertToVideoModel(Optional.empty(), videoMediaItem);

    assertEquals(
        videoModel.getUploadedTime(), GoogleMediaItem.parseIso8601DateTime(fakePhotosApiTimestamp));
  }

  @Test
  public void getUploadTime_photoModel() throws ParseException {
    String fakePhotosApiTimestamp = "2014-10-02T15:01:23.045123456Z";

    GoogleMediaItem photoMediaItem = getPhotoMediaItem();
    MediaMetadata metadata = new MediaMetadata();
    metadata.setPhoto(new Photo());
    // CreationTime in GoogleMediaItem is populated as uploadTime in our common models.
    metadata.setCreationTime(fakePhotosApiTimestamp);
    photoMediaItem.setMediaMetadata(metadata);

    PhotoModel photoModel = GoogleMediaItem.convertToPhotoModel(Optional.empty(), photoMediaItem);

    assertEquals(
        photoModel.getUploadedTime(), GoogleMediaItem.parseIso8601DateTime(fakePhotosApiTimestamp));
  }

  public static GoogleMediaItem getPhotoMediaItem() {
    MediaMetadata photoMetadata = new MediaMetadata();
    photoMetadata.setPhoto(new Photo());
    photoMetadata.setCreationTime("2022-09-01T20:25:38Z");

    GoogleMediaItem photoMediaItem = new GoogleMediaItem();
    photoMediaItem.setMimeType("image/png");
    photoMediaItem.setDescription("Description");
    photoMediaItem.setFilename("filename.png");
    photoMediaItem.setBaseUrl("https://www.google.com");
    photoMediaItem.setId("photo_id");
    photoMediaItem.setMediaMetadata(photoMetadata);
    return photoMediaItem;
  }

  public static GoogleMediaItem getVideoMediaItem() {
    MediaMetadata videoMetadata = new MediaMetadata();
    videoMetadata.setVideo(new Video());
    videoMetadata.setCreationTime("2022-09-01T20:25:38Z");

    GoogleMediaItem videoMediaItem = new GoogleMediaItem();
    videoMediaItem.setMimeType("video/mp4");
    videoMediaItem.setDescription("Description");
    videoMediaItem.setFilename("filename.mp4");
    videoMediaItem.setBaseUrl("https://www.google.com");
    videoMediaItem.setId("video_id");
    videoMediaItem.setMediaMetadata(videoMetadata);
    return videoMediaItem;
  }
}
