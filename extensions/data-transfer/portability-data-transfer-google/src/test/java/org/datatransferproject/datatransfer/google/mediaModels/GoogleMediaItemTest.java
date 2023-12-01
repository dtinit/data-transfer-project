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
  public void getMimeType_photoModel_mimeTypeFromFilename() throws Exception {
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

      try{
      PhotoModel photoModel = GoogleMediaItem.convertToPhotoModel(Optional.empty(), photoMediaItem);

      assertEquals(entry.getValue(), photoModel.getMimeType());
      }catch(Exception e)
      {
        fail(e.getMessage());
      }
    }
  }

  @Test
  public void getMimeType_videoModel_mimeTypeFromFilename() {
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

      try {
        VideoModel videoModel = GoogleMediaItem.convertToVideoModel(Optional.empty(),
            videoMediaItem);

        assertEquals(entry.getValue(), videoModel.getMimeType());
      }
      catch(Exception e)
      {
        fail(e.getMessage());
      }
    }
  }

  @Test
  public void getMimeType_photoModel_filenameMimeTypeIsNull() {
    GoogleMediaItem photoMediaItem = getPhotoMediaItem();
    photoMediaItem.setFilename("file");
    photoMediaItem.setMimeType("image/webp");

    try {
      PhotoModel photoModel = GoogleMediaItem.convertToPhotoModel(Optional.empty(), photoMediaItem);

      assertEquals("image/webp", photoModel.getMimeType());
    }
    catch(Exception e)
    {
      fail(e.getMessage());
    }
  }

  @Test
  public void getMimeType_videoModel_filenameMimeTypeIsNull() {
    GoogleMediaItem videoMediaItem = getVideoMediaItem();
    videoMediaItem.setFilename("file");
    videoMediaItem.setMimeType("video/webm");

    try {
      VideoModel videoModel = GoogleMediaItem.convertToVideoModel(Optional.empty(), videoMediaItem);

      assertEquals("video/webm", videoModel.getMimeType());
    }catch(Exception e)
    {
      fail(e.getMessage());
    }
  }

  @Test
  public void getMimeType_photoModel_nullMimeTypeReturnsDefault() {
    GoogleMediaItem photoMediaItem = getPhotoMediaItem();
    photoMediaItem.setFilename("file");
    photoMediaItem.setMimeType(null);

    try{
    PhotoModel photoModel = GoogleMediaItem.convertToPhotoModel(Optional.empty(), photoMediaItem);

    // Default defined in GoogleMediaItem
    assertEquals("image/jpg", photoModel.getMimeType());
    }catch(Exception e)
    {
      fail(e.getMessage());
    }
  }

  @Test
  public void getMimeType_videoModel_nullMimeTypeReturnsDefault() {
    GoogleMediaItem videoMediaItem = getVideoMediaItem();
    videoMediaItem.setFilename("file");
    videoMediaItem.setMimeType(null);

    try{
    VideoModel videoModel = GoogleMediaItem.convertToVideoModel(Optional.empty(), videoMediaItem);

    // Default defined in GoogleMediaItem
    assertEquals("video/mp4", videoModel.getMimeType());
    }catch(Exception e)
    {
      fail(e.getMessage());
    }
  }

  @Test
  public void getMimeType_photoModel_unsupportedFileExtension() {
    GoogleMediaItem photoMediaItem = getPhotoMediaItem();
    photoMediaItem.setFilename("file.avif");
    photoMediaItem.setMimeType("image/png");

    try{
    PhotoModel photoModel = GoogleMediaItem.convertToPhotoModel(Optional.empty(), photoMediaItem);

    // defaults to the mimetype that is already set, as avif is not handled.
    assertEquals("image/png", photoModel.getMimeType());
    }catch(Exception e)
    {
      fail(e.getMessage());
    }
  }

  @Test
  public void getUploadTime_videoModel() {
    GoogleMediaItem videoMediaItem = getVideoMediaItem();
    MediaMetadata metadata = new MediaMetadata();
    metadata.setVideo(new Video());
    metadata.setCreationTime("2023-10-02T22:33:38Z");
    videoMediaItem.setMediaMetadata(metadata);

    try{
    VideoModel videoModel = GoogleMediaItem.convertToVideoModel(Optional.empty(), videoMediaItem);

    assertEquals("Mon Oct 02 22:33:38 UTC 2023", videoModel.getUploadedTime().toString());
    }catch(Exception e)
    {
      fail(e.getMessage());
    }
  }

  @Test
  public void getUploadTime_photoModel() {
    GoogleMediaItem photoMediaItem = getPhotoMediaItem();
    MediaMetadata metadata = new MediaMetadata();
    metadata.setPhoto(new Photo());
    metadata.setCreationTime("2023-10-02T22:33:38Z");
    photoMediaItem.setMediaMetadata(metadata);

    try{
    PhotoModel photoModel = GoogleMediaItem.convertToPhotoModel(Optional.empty(), photoMediaItem);

    assertEquals("Mon Oct 02 22:33:38 UTC 2023", photoModel.getUploadedTime().toString());
    }catch(Exception e)
    {
      fail(e.getMessage());
    }
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
