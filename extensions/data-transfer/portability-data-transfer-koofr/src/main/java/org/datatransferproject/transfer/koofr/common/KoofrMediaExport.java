package org.datatransferproject.transfer.koofr.common;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.spi.transfer.types.InvalidTokenException;
import org.datatransferproject.types.common.models.photos.PhotoAlbum;
import org.datatransferproject.types.common.models.photos.PhotoModel;
import org.datatransferproject.types.common.models.videos.VideoAlbum;
import org.datatransferproject.types.common.models.videos.VideoObject;

public class KoofrMediaExport {

  protected final KoofrClient koofrClient;
  protected final Monitor monitor;

  protected ArrayList<PhotoAlbum> albums;
  protected HashSet<String> albumsWithPhotos;
  protected ArrayList<PhotoModelContainer> photos;
  protected HashSet<String> albumsWithVideos;
  protected ArrayList<VideoObjectContainer> videos;
  protected String rootPath;
  protected List<FilesListRecursiveItem> items;

  public KoofrMediaExport(KoofrClient koofrClient, Monitor monitor) {
    this.koofrClient = koofrClient;
    this.monitor = monitor;
  }

  public void export() throws IOException, InvalidTokenException {
    albums = new ArrayList<>();

    albumsWithPhotos = new HashSet<>();
    photos = new ArrayList<>();

    albumsWithVideos = new HashSet<>();
    videos = new ArrayList<>();

    rootPath = koofrClient.getRootPath();
    items = koofrClient.listRecursive(rootPath);

    processItems();
  }

  public List<PhotoAlbum> getPhotoAlbums() {
    ArrayList<PhotoAlbum> exportAlbums = new ArrayList<>();

    for (PhotoAlbum album : albums) {
      if (albumsWithPhotos.contains(album.getId())) {
        exportAlbums.add(album);
      }
    }

    return exportAlbums;
  }

  public List<PhotoModel> getPhotos() throws IOException, InvalidTokenException {
    ArrayList<PhotoModel> exportPhotos = new ArrayList<>();

    for (PhotoModelContainer photoContainer : photos) {
      PhotoModel photo = photoContainer.photoModel;

      String fetchableUrl = getFetchableUrl(photoContainer.fullPath);

      if (fetchableUrl == null) {
        continue;
      }

      exportPhotos.add(
          new PhotoModel(
              photo.getTitle(),
              fetchableUrl,
              photo.getDescription(),
              photo.getMediaType(),
              photo.getDataId(),
              photo.getAlbumId(),
              photo.isInTempStore(),
              photo.getUploadedTime()));
    }

    return exportPhotos;
  }

  public List<VideoAlbum> getVideoAlbums() {
    ArrayList<VideoAlbum> exportAlbums = new ArrayList<>();

    for (PhotoAlbum album : albums) {
      if (albumsWithVideos.contains(album.getId())) {
        exportAlbums.add(new VideoAlbum(album.getId(), album.getName(), album.getDescription()));
      }
    }

    return exportAlbums;
  }

  public List<VideoObject> getVideos() throws IOException, InvalidTokenException {
    ArrayList<VideoObject> exportVideos = new ArrayList<>();

    for (VideoObjectContainer container : videos) {
      VideoObject video = container.videoObject;

      String fetchableUrl = getFetchableUrl(container.fullPath);

      if (fetchableUrl == null) {
        continue;
      }

      exportVideos.add(
          new VideoObject(
              video.getName(),
              fetchableUrl,
              video.getDescription(),
              video.getEncodingFormat(),
              video.getDataId(),
              video.getAlbumId(),
              video.isInTempStore()));
    }

    return exportVideos;
  }

  protected void processItems() {
    for (FilesListRecursiveItem item : items) {
      processItem(item);
    }
  }

  protected void processItem(FilesListRecursiveItem item) {
    if (FilesListRecursiveItem.TYPE_FILE.equals(item.getType())) {
      processItemFile(item.getFile(), item.getPath());
    } else if (FilesListRecursiveItem.TYPE_ERROR.equals(item.getType())) {
      monitor.severe(
          () ->
              String.format(
                  "Koofr list item error: %s: %s: %s",
                  item.getPath(), item.getError().getCode(), item.getError().getMessage()));
    }
  }

  protected void processItemFile(FilesFile file, String path) {
    // For albums pathParts will be ["", "Album name"].
    // For media files pathParts will be ["", "Album name", "medianame.ext"] or
    // ["", "Album name", "subdir1", "subdir2", "medianame.ext"].
    String[] pathParts = path.split("/");

    if (FilesFile.TYPE_DIR.equals(file.getType())) {
      if (pathParts.length == 2 && !pathParts[1].isEmpty()) {
        processAlbum(file, path);
      }
    } else if (pathParts.length > 2 && file.getContentType().startsWith("image/")) {
      String albumId = getAlbumId(pathParts);

      processPhoto(file, path, albumId);
    } else if (pathParts.length > 2 && file.getContentType().startsWith("video/")) {
      String albumId = getAlbumId(pathParts);

      processVideo(file, path, albumId);
    }
  }

  protected void processAlbum(FilesFile file, String path) {
    String albumId = path;
    String albumName = getFileName(file);
    String description = getFileDescription(file);

    albums.add(new PhotoAlbum(albumId, albumName, description));
  }

  protected void processPhoto(FilesFile file, String path, String albumId) {
    String photoId = path;
    String name = getFileName(file);
    String description = getFileDescription(file);
    String contentType = file.getContentType();
    Date uploadedTime = new Date(file.getModified());
    String fullPath = rootPath + path;

    albumsWithPhotos.add(albumId);

    PhotoModelContainer container = new PhotoModelContainer();
    container.photoModel =
        new PhotoModel(name, "", description, contentType, photoId, albumId, false, uploadedTime);
    container.fullPath = fullPath;

    photos.add(container);
  }

  protected void processVideo(FilesFile file, String path, String albumId) {
    String videoId = path;
    String name = getFileName(file);
    String description = getFileDescription(file);
    String contentType = file.getContentType();
    String fullPath = rootPath + path;

    albumsWithVideos.add(albumId);

    VideoObjectContainer container = new VideoObjectContainer();
    container.videoObject =
        new VideoObject(name, "", description, contentType, videoId, albumId, false);
    container.fullPath = fullPath;

    videos.add(container);
  }

  protected String getAlbumId(String[] pathParts) {
    // pathParts can be ["", "Album name", "medianame.ext"] or
    // ["", "Album name", "subdir1", "subdir2", "medianame.ext"]
    // In both cases the albumId will be "/"
    return String.join("/", Arrays.copyOfRange(pathParts, 0, 2));
  }

  protected String getFileName(FilesFile file) {
    String albumName = file.getName();

    if (file.getTags() != null && file.getTags().containsKey("originalName")) {
      albumName = file.getTags().get("originalName").get(0);
    }

    return albumName;
  }

  protected String getFileDescription(FilesFile file) {
    String description = null;

    if (file.getTags() != null && file.getTags().containsKey("description")) {
      description = file.getTags().get("description").get(0);
    }

    return description;
  }

  protected String getFetchableUrl(String fullPath) throws IOException, InvalidTokenException {
    try {
      return koofrClient.fileLink(fullPath);
    } catch (IOException e) {
      monitor.severe(() -> String.format("Koofr file link error: %s", fullPath), e);
      return null;
    }
  }

  public static class PhotoModelContainer {
    PhotoModel photoModel;
    String fullPath;
  }

  public static class VideoObjectContainer {
    VideoObject videoObject;
    String fullPath;
  }
}
