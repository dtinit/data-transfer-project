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
package org.dataportabilityproject.serviceProviders.flickr;

import static com.google.common.base.Preconditions.checkState;

import com.flickr4java.flickr.Flickr;
import com.flickr4java.flickr.FlickrException;
import com.flickr4java.flickr.REST;
import com.flickr4java.flickr.RequestContext;
import com.flickr4java.flickr.auth.Auth;
import com.flickr4java.flickr.photos.Photo;
import com.flickr4java.flickr.photos.PhotoList;
import com.flickr4java.flickr.photos.PhotosInterface;
import com.flickr4java.flickr.photosets.Photoset;
import com.flickr4java.flickr.photosets.Photosets;
import com.flickr4java.flickr.photosets.PhotosetsInterface;
import com.flickr4java.flickr.uploader.UploadMetaData;
import com.flickr4java.flickr.uploader.Uploader;
import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
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
import org.dataportabilityproject.shared.AppCredentials;
import org.dataportabilityproject.shared.IdOnlyResource;

public class FlickrPhotoService implements
    Exporter<PhotosModelWrapper>,
    Importer<PhotosModelWrapper> {

  private static final String CACHE_ALBUM_METADATA_PREFIX = "meta-";
  private static final int PHOTO_SETS_PER_PAGE = 500;
  private static final int PHOTO_PER_PAGE = 50;
  private static final List<String> EXTRAS =
      ImmutableList.of("url_o", "o_dims", "original_format");
  private static final String PHOTOSET_EXTRAS = "";

  private final Flickr flickr;
  private final PhotosetsInterface photosetsInterface;
  private final PhotosInterface photosInterface;
  private final Uploader uploader;
  private final JobDataCache jobDataCache;
  private Auth auth;

  FlickrPhotoService(AppCredentials appCredentials, Auth auth,
      JobDataCache jobDataCache) throws IOException {
    this.flickr = new Flickr(appCredentials.key(), appCredentials.secret(), new REST());
    this.photosetsInterface = flickr.getPhotosetsInterface();
    this.photosInterface = flickr.getPhotosInterface();
    this.uploader = flickr.getUploader();
    this.jobDataCache = jobDataCache;
    this.auth = auth;
    RequestContext.getRequestContext().setAuth(auth);
  }

  private static int getPage(Optional<PaginationInformation> paginationInformation) {
    return paginationInformation.map(
        paginationInformation1 -> ((FlickrPaginationInformation) paginationInformation1)
            .getPage()).orElse(1);
  }

  private static PhotoModel toCommonPhoto(Photo p, String albumId) {
    checkState(!Strings.isNullOrEmpty(p.getOriginalSize().getSource()),
        "photo %s had a null authUrl", p.getId());
    return new PhotoModel(
        p.getTitle(),
        p.getOriginalSize().getSource(),
        p.getDescription(),
        toMimeType(p.getOriginalFormat()),
        albumId);
  }

  private static String toMimeType(String flickrFormat) {
    switch (flickrFormat) {
      case "jpg":
      case "jpeg":
        return "image/jpeg";
      default:
        throw new IllegalArgumentException("Don't know how to map: " + flickrFormat);
    }
  }

  @Override
  public void importItem(PhotosModelWrapper modelWrapper) throws IOException {
    PhotosetsInterface photosetsInterface = flickr.getPhotosetsInterface();
    try {
      for (PhotoAlbum album : modelWrapper.getAlbums()) {
        // Store the data in the cache because Flickr only allows you
        // to create an album with a photo in it so we need to wait for
        // the first photo to create the album.
        jobDataCache.store(CACHE_ALBUM_METADATA_PREFIX + album.getId(), album);
      }
      for (PhotoModel photo : modelWrapper.getPhotos()) {
        String photoId = uploadPhoto(photo);
        String oldAlbumId = photo.getAlbumId();
        if (!jobDataCache.hasKey(oldAlbumId)) {
          PhotoAlbum album = jobDataCache.getData(
              CACHE_ALBUM_METADATA_PREFIX + oldAlbumId,
              PhotoAlbum.class);
          Photoset photoset = photosetsInterface.create("Copy of - " + album.getName(),
              album.getDescription(), photoId);
          jobDataCache.store(oldAlbumId, photoset.getId());
        } else {
          String newAlbumId = jobDataCache.getData(oldAlbumId, String.class);
          photosetsInterface.addPhoto(newAlbumId, photoId);
        }
      }
    } catch (FlickrException e) {
      throw new IOException("Problem communicating with serviceProviders.flickr", e);
    }
  }

  @Override
  public PhotosModelWrapper export(ExportInformation exportInformation)
      throws IOException {
    Optional<Resource> resource = exportInformation.getResource();
    if (resource.isPresent()) {
      IdOnlyResource flickrResource = (IdOnlyResource) resource.get();
      return getPhotos(
          flickrResource.getId(),
          exportInformation.getPaginationInformation());
    } else {
      return getAlbums(exportInformation.getPaginationInformation());
    }
  }

  private PhotosModelWrapper getAlbums(
      Optional<PaginationInformation> paginationInformation) throws IOException {
    try {
      ImmutableList.Builder<PhotoAlbum> results = ImmutableList.builder();
      List<IdOnlyResource> subResources = new ArrayList<>();

      int page = getPage(paginationInformation);
      Photosets photoSetList = photosetsInterface.getList(
          auth.getUser().getId(),
          PHOTO_SETS_PER_PAGE,
          page,
          PHOTOSET_EXTRAS);
      for (Photoset photoset : photoSetList.getPhotosets()) {
        // Saving data to the album allows the target service
        // to recreate the album structure.
        results.add(new PhotoAlbum(
            photoset.getId(),
            photoset.getTitle(),
            photoset.getDescription()));
        // Adding sub-resources tells the framework to re-call
        // export to get all the photos.
        subResources.add(new IdOnlyResource(photoset.getId()));
      }

      FlickrPaginationInformation newPage = null;
      boolean hasMore = photoSetList.getPage() != photoSetList.getPages()
          && !photoSetList.getPhotosets().isEmpty();
      if (hasMore) {
        newPage = new FlickrPaginationInformation(page + 1);
      }

      return new PhotosModelWrapper(
          results.build(),
          null,
          new ContinuationInformation(subResources, newPage));
    } catch (FlickrException e) {
      throw new IOException("Couldn't fetch albums", e);
    }
  }

  private PhotosModelWrapper getPhotos(String photosetId,
      Optional<PaginationInformation> paginationInformation) throws IOException {
    try {
      int page = getPage(paginationInformation);
      PhotoList<Photo> photoSetList;

      if (null == photosetId) {
        RequestContext.getRequestContext().setExtras(EXTRAS);
        photoSetList = photosInterface.getNotInSet(PHOTO_PER_PAGE, page);
        RequestContext.getRequestContext().setExtras(ImmutableList.of());
      } else {
        photoSetList = photosetsInterface.getPhotos(photosetId,
            ImmutableSet.copyOf(EXTRAS),
            0, PHOTO_PER_PAGE, page);
      }
      boolean hasMore = photoSetList.getPage() != photoSetList.getPages()
          && !photoSetList.isEmpty();

      Collection<PhotoModel> photos = photoSetList.stream()
          .map(p -> toCommonPhoto(p, photosetId))
          .collect(Collectors.toList());
      FlickrPaginationInformation newPage = null;
      if (hasMore) {
        newPage = new FlickrPaginationInformation(page + 1);
      }
      return new PhotosModelWrapper(
          null,
          photos,
          new ContinuationInformation(null, newPage));
    } catch (FlickrException e) {
      throw new IOException("Couldn't fetch photos in album: " + photosetId, e);
    }
  }

  private InputStream getImageAsStream(String urlStr) throws IOException {
    URL url = new URL(urlStr);
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    conn.connect();
    return conn.getInputStream();
  }

  private String uploadPhoto(PhotoModel photo)
      throws IOException, FlickrException {
    BufferedInputStream inStream = new BufferedInputStream(
        getImageAsStream(photo.getFetchableUrl()));
    UploadMetaData uploadMetaData = new UploadMetaData()
        .setAsync(false)
        .setPublicFlag(false)
        .setFriendFlag(false)
        .setFamilyFlag(false)
        .setTitle("copy of - " + photo.getTitle())
        .setDescription(photo.getDescription());
    return uploader.upload(inStream, uploadMetaData);
  }
}
