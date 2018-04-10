/*
 * Copyright 2018 The Data Transfer Project Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.dataportabilityproject.datatransfer.flickr.photos;

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
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.dataportabilityproject.spi.transfer.provider.ExportResult;
import org.dataportabilityproject.spi.transfer.provider.ExportResult.ResultType;
import org.dataportabilityproject.spi.transfer.provider.Exporter;
import org.dataportabilityproject.spi.transfer.types.ContinuationData;
import org.dataportabilityproject.spi.transfer.types.ExportInformation;
import org.dataportabilityproject.spi.transfer.types.IdOnlyContainerResource;
import org.dataportabilityproject.spi.transfer.types.IntPaginationToken;
import org.dataportabilityproject.spi.transfer.types.PaginationData;
import org.dataportabilityproject.types.transfer.auth.AppCredentials;
import org.dataportabilityproject.types.transfer.auth.AuthData;
import org.dataportabilityproject.types.transfer.models.photos.PhotoAlbum;
import org.dataportabilityproject.types.transfer.models.photos.PhotoModel;
import org.dataportabilityproject.types.transfer.models.photos.PhotosContainerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class FlickrPhotosExporter implements Exporter<AuthData, PhotosContainerResource> {
  private static final int PHOTO_PER_PAGE = 50;
  private static final List<String> EXTRAS = ImmutableList.of("url_o", "o_dims", "original_format");
  private static final int PHOTO_SETS_PER_PAGE = 500;
  private static final String PHOTOSET_EXTRAS = "";

  private final PhotosetsInterface photosetsInterface;
  private final PhotosInterface photosInterface;
  private final Flickr flickr;

  private final Logger logger = LoggerFactory.getLogger(FlickrPhotosExporter.class);

  public FlickrPhotosExporter(AppCredentials appCredentials) {
    this.flickr = new Flickr(appCredentials.getKey(), appCredentials.getSecret(), new REST());
    this.photosetsInterface = flickr.getPhotosetsInterface();
    this.photosInterface = flickr.getPhotosInterface();
  }

  @VisibleForTesting
  FlickrPhotosExporter(Flickr flickr) {
    this.flickr = flickr;
    this.photosInterface = flickr.getPhotosInterface();
    this.photosetsInterface = flickr.getPhotosetsInterface();
  }

  @VisibleForTesting
  static PhotoModel toCommonPhoto(Photo p, String albumId) {
    Preconditions.checkArgument(
        !Strings.isNullOrEmpty(p.getOriginalSize().getSource()),
        "Photo [" + p.getId() + "] has a null authUrl");
    return new PhotoModel(
        p.getTitle(),
        p.getOriginalSize().getSource(),
        p.getDescription(),
        toMimeType(p.getOriginalFormat()),
        albumId);
  }

  @VisibleForTesting
  static String toMimeType(String flickrFormat) {
    switch (flickrFormat) {
      case "jpg":
      case "jpeg":
        return "image/jpeg";
      case "png":
        return "image/png";
      default:
        throw new IllegalArgumentException("Don't know how to map: " + flickrFormat);
    }
  }

  @Override
  public ExportResult<PhotosContainerResource> export(UUID jobId, AuthData authData) {
    return export(jobId, authData, new ExportInformation(null, null));
  }

  @Override
  public ExportResult<PhotosContainerResource> export(
      UUID jobId, AuthData authData, ExportInformation exportInformation) {
    Auth auth;
    try {
      auth = FlickrUtils.getAuth(authData, flickr);
    } catch (FlickrException e) {
      return new ExportResult<>(ResultType.ERROR, "Error authorizing user: " + e.getErrorMessage());
    }

    RequestContext.getRequestContext().setAuth(auth);
    PaginationData paginationData = exportInformation.getPaginationData();

    IdOnlyContainerResource resource =
        (IdOnlyContainerResource) exportInformation.getContainerResource();
    if (resource != null) {
      return getPhotos(resource, paginationData);
    } else {
      return getAlbums(paginationData, auth);
    }
  }

  private ExportResult<PhotosContainerResource> getPhotos(
      IdOnlyContainerResource resource, PaginationData paginationData) {
    String photoSetId = resource.getId();
    int page = paginationData == null ? 1 : ((IntPaginationToken) paginationData).getStart();
    PhotoList<Photo> photoSetList;

    try {
      if (photoSetId == null) {
        RequestContext.getRequestContext().setExtras(EXTRAS);
        photoSetList = photosInterface.getNotInSet(PHOTO_PER_PAGE, page);
        RequestContext.getRequestContext().setExtras(ImmutableList.of());
      } else {
        photoSetList =
            photosetsInterface.getPhotos(
                photoSetId, ImmutableSet.copyOf(EXTRAS), 0, PHOTO_PER_PAGE, page);
      }
    } catch (FlickrException e) {
      return new ExportResult<>(
          ResultType.ERROR, "Error exporting Flickr photo: " + e.getErrorMessage());
    }

    boolean hasMore = photoSetList.getPage() != photoSetList.getPages() && !photoSetList.isEmpty();

    Collection<PhotoModel> photos =
        photoSetList.stream().map(p -> toCommonPhoto(p, photoSetId)).collect(Collectors.toList());

    PaginationData newPage = null;
    if (hasMore) {
      newPage = new IntPaginationToken(page + 1);
    }

    // Get result type
    ResultType resultType = ResultType.CONTINUE;
    if (newPage == null) {
      resultType = ResultType.END;
    }

    PhotosContainerResource photosContainerResource = new PhotosContainerResource(null, photos);
    return new ExportResult<>(resultType, photosContainerResource, new ContinuationData(newPage));
  }

  private ExportResult<PhotosContainerResource> getAlbums(
      PaginationData paginationData, Auth auth) {
    ImmutableList.Builder<PhotoAlbum> albumBuilder = ImmutableList.builder();
    List<IdOnlyContainerResource> subResources = new ArrayList<>();

    int page = paginationData == null ? 1 : ((IntPaginationToken) paginationData).getStart();
    Photosets photoSetList;

    try {
      photoSetList =
          photosetsInterface.getList(
              auth.getUser().getId(), PHOTO_SETS_PER_PAGE, page, PHOTOSET_EXTRAS);
    } catch (FlickrException e) {
      return new ExportResult<>(
          ResultType.ERROR, "Error exporting Flickr album: " + e.getErrorMessage());
    }

    for (Photoset photoSet : photoSetList.getPhotosets()) {
      // Saving data to the album allows the target service to recreate the album structure
      albumBuilder.add(
          new PhotoAlbum(photoSet.getId(), photoSet.getTitle(), photoSet.getDescription()));
      // Adding subresources tells the framework to recall export to get all the photos
      subResources.add(new IdOnlyContainerResource(photoSet.getId()));
    }

    PaginationData newPage = null;
    boolean hasMore =
        photoSetList.getPage() != photoSetList.getPages() && !photoSetList.getPhotosets().isEmpty();
    if (hasMore) newPage = new IntPaginationToken(page + 1);

    PhotosContainerResource photosContainerResource =
        new PhotosContainerResource(albumBuilder.build(), null);
    ContinuationData continuationData = new ContinuationData(newPage);
    subResources.forEach(resource -> continuationData.addContainerResource(resource));

    // Get result type
    ResultType resultType = ResultType.CONTINUE;
    if (newPage == null) {
      resultType = ResultType.END;
    }

    return new ExportResult<>(resultType, photosContainerResource, continuationData);
  }
}
