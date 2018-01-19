/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.dataportabilityproject.serviceProviders.google.piccasa;

import com.google.api.client.auth.oauth2.Credential;
import com.google.gdata.client.photos.PicasawebService;
import com.google.gdata.data.MediaContent;
import com.google.gdata.data.PlainTextConstruct;
import com.google.gdata.data.media.MediaStreamSource;
import com.google.gdata.data.photos.AlbumEntry;
import com.google.gdata.data.photos.AlbumFeed;
import com.google.gdata.data.photos.GphotoEntry;
import com.google.gdata.data.photos.PhotoEntry;
import com.google.gdata.data.photos.UserFeed;
import com.google.gdata.util.ServiceException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.dataportabilityproject.cloud.interfaces.JobDataCache;
import org.dataportabilityproject.dataModels.ContinuationInformation;
import org.dataportabilityproject.dataModels.ExportInformation;
import org.dataportabilityproject.dataModels.Exporter;
import org.dataportabilityproject.dataModels.Importer;
import org.dataportabilityproject.dataModels.PaginationInformation;
import org.dataportabilityproject.dataModels.Resource;
import org.dataportabilityproject.dataModels.photos.PhotoAlbum;
import org.dataportabilityproject.dataModels.photos.PhotoModel;
import org.dataportabilityproject.dataModels.photos.PhotosModelWrapper;
import org.dataportabilityproject.shared.IdOnlyResource;

public class GooglePhotosService
    implements Exporter<PhotosModelWrapper>, Importer<PhotosModelWrapper> {

  private static final String CLIENT_NAME = "Portability";

  private final PicasawebService service;
  private final JobDataCache jobDataCache;

  public GooglePhotosService(Credential credential, JobDataCache jobDataCache) {
    this.jobDataCache = jobDataCache;
    this.service = new PicasawebService(CLIENT_NAME);
    this.service.setOAuth2Credentials(credential);
  }

  private static InputStream getImageAsStream(String urlStr) throws IOException {
    URL url = new URL(urlStr);
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    conn.connect();
    return conn.getInputStream();
  }

  @Override
  public PhotosModelWrapper export(ExportInformation exportInformation) throws IOException {
    if (exportInformation.getResource().isPresent()) {
      return exportPhotos(
          ((IdOnlyResource) exportInformation.getResource().get()).getId(),
          exportInformation.getPaginationInformation());
    } else {
      return exportAlbums(exportInformation.getPaginationInformation());
    }

  }

  private PhotosModelWrapper exportAlbums(Optional<PaginationInformation> pageInfo)
      throws IOException {
    URL albumUrl = new URL("https://picasaweb.google.com/data/feed/api/user/default?kind=album");

    UserFeed albumFeed;

    try {
      albumFeed = service.getFeed(albumUrl, UserFeed.class);
    } catch (ServiceException e) {
      throw new IOException("Problem making request to: " + albumUrl, e);
    }

    List<PhotoAlbum> albums = new ArrayList<>(albumFeed.getEntries().size());
    List<Resource> resources = new ArrayList<>(albumFeed.getEntries().size());

    for (GphotoEntry myAlbum : albumFeed.getEntries()) {
      // Adding sub-resources tells the framework to re-call
      // export to get all the photos.
      resources.add(new IdOnlyResource(myAlbum.getGphotoId()));
      // Saving data to the album allows the target service
      // to recreate the album structure.
      albums.add(new PhotoAlbum(
          myAlbum.getGphotoId(),
          myAlbum.getTitle().getPlainText(),
          myAlbum.getDescription().getPlainText()
      ));
    }

    return new PhotosModelWrapper(albums, null, new ContinuationInformation(resources, null));
  }

  private PhotosModelWrapper exportPhotos(
      String albumId, Optional<PaginationInformation> pageInfo) throws IOException {
    // imgmax=d gets the original immage as per:
    // https://developers.google.com/picasa-web/docs/2.0/reference
    URL photosUrl = new URL("https://picasaweb.google.com/data/feed/api/user/default/albumid/"
        + albumId + "?imgmax=d");
    AlbumFeed photoFeed;
    try {
      photoFeed = service.getFeed(photosUrl, AlbumFeed.class);
    } catch (ServiceException e) {
      throw new IOException("Problem making request to: " + photosUrl, e);
    }

    List<PhotoModel> photos = new ArrayList<>(photoFeed.getEntries().size());
    for (GphotoEntry photo : photoFeed.getEntries()) {
      MediaContent mediaContent = (MediaContent) photo.getContent();
      photos.add(new PhotoModel(
          photo.getTitle().getPlainText(),
          mediaContent.getUri(),
          photo.getDescription().getPlainText(),
          mediaContent.getMimeType().getMediaType(),
          albumId
      ));
    }

    return new PhotosModelWrapper(null, photos, new ContinuationInformation(null, null));
  }

  @Override
  public void importItem(PhotosModelWrapper wrapper) throws IOException {
    for (PhotoAlbum album : wrapper.getAlbums()) {
      if (true) {
        // Google doesn't support creating albums anymore
        continue;
      }
      AlbumEntry myAlbum = new AlbumEntry();

      myAlbum.setTitle(new PlainTextConstruct("copy of " + album.getName()));
      myAlbum.setDescription(new PlainTextConstruct(album.getDescription()));

      URL albumUrl = new URL(
          "https://picasaweb.google.com/data/feed/api/user/default");
      AlbumEntry insertedEntry;

      try {
        // https://developers.google.com/picasa-web/docs/2.0/developers_guide_java#AddAlbums
        insertedEntry = service.insert(albumUrl, myAlbum);
        jobDataCache.store(album.getId(), insertedEntry.getGphotoId());
      } catch (ServiceException e) {
        throw new IOException(
            "Problem copying" + album.getName() + " request to: " + albumUrl, e);
      }
    }

    for (PhotoModel photo : wrapper.getPhotos()) {
      //String newAlbumId = jobDataCache.getData(photo.getAlbumId(), String.class);
      String newAlbumId = "default";
      URL photoPostUrl = new URL("https://picasaweb.google.com/data/feed/api/user/default/albumid/"
          + newAlbumId);

      PhotoEntry myPhoto = new PhotoEntry();
      myPhoto.setTitle(new PlainTextConstruct("copy of " + photo.getTitle()));
      myPhoto.setDescription(new PlainTextConstruct(photo.getDescription()));
      myPhoto.setClient(CLIENT_NAME);

      String mediaType = photo.getMediaType();

      if (mediaType == null) {
        mediaType = "image/jpeg";
      }

      MediaStreamSource streamSource = new MediaStreamSource(
          getImageAsStream(photo.getFetchableUrl()),
          mediaType);
      myPhoto.setMediaSource(streamSource);

      try {
        service.insert(photoPostUrl, myPhoto);
      } catch (ServiceException e) {
        throw new IOException("Problem adding " + photo.getTitle() + " to "
            + newAlbumId, e);
      }
    }
  }
}
