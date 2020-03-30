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

package org.datatransferproject.datatransfer.flickr.photos;

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
import com.google.common.util.concurrent.RateLimiter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.datatransferproject.spi.transfer.provider.ExportResult;
import org.datatransferproject.spi.transfer.provider.ExportResult.ResultType;
import org.datatransferproject.spi.transfer.provider.Exporter;
import org.datatransferproject.spi.transfer.types.ContinuationData;
import org.datatransferproject.types.common.ExportInformation;
import org.datatransferproject.types.common.IntPaginationToken;
import org.datatransferproject.types.common.PaginationData;
import org.datatransferproject.types.common.models.IdOnlyContainerResource;
import org.datatransferproject.types.common.models.photos.PhotoAlbum;
import org.datatransferproject.types.common.models.photos.PhotoModel;
import org.datatransferproject.types.common.models.photos.PhotosContainerResource;
import org.datatransferproject.types.transfer.auth.AppCredentials;
import org.datatransferproject.types.transfer.auth.AuthData;
import org.datatransferproject.types.transfer.serviceconfig.TransferServiceConfig;

public class FlickrPhotosExporter implements Exporter<AuthData, PhotosContainerResource> {

  private static final int PHOTO_PER_PAGE = 50;
  private static final List<String> EXTRAS = ImmutableList.of("url_o", "o_dims", "original_format");
  private static final int PHOTO_SETS_PER_PAGE = 500;
  private static final String PHOTOSET_EXTRAS = "";

  private final PhotosetsInterface photosetsInterface;
  private final PhotosInterface photosInterface;
  private final Flickr flickr;
  private final RateLimiter perUserRateLimiter;

  public FlickrPhotosExporter(AppCredentials appCredentials, TransferServiceConfig serviceConfig) {
    this.flickr = new Flickr(appCredentials.getKey(), appCredentials.getSecret(), new REST());
    this.photosetsInterface = flickr.getPhotosetsInterface();
    this.photosInterface = flickr.getPhotosInterface();
    this.perUserRateLimiter = serviceConfig.getPerUserRateLimiter();
  }

  @VisibleForTesting
  FlickrPhotosExporter(Flickr flickr, TransferServiceConfig serviceConfig) {
    this.flickr = flickr;
    this.photosInterface = flickr.getPhotosInterface();
    this.photosetsInterface = flickr.getPhotosetsInterface();
    this.perUserRateLimiter = serviceConfig.getPerUserRateLimiter();
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
        p.getId(),
        albumId,
        false);
  }

  @VisibleForTesting
  static String toMimeType(String flickrFormat) {
    switch (flickrFormat) {
      case "jpg":
      case "jpeg":
        return "image/jpeg";
      case "png":
        return "image/png";
      case "gif":
        return "image/gif";
      default:
        throw new IllegalArgumentException("Don't know how to map: " + flickrFormat);
    }
  }

  @Override
  public ExportResult<PhotosContainerResource> export(
      UUID jobId, AuthData authData, Optional<ExportInformation> exportInformation) {
    Auth auth;
    try {
      auth = FlickrUtils.getAuth(authData, flickr);
    } catch (FlickrException e) {
      return new ExportResult<>(e);
    }

    RequestContext.getRequestContext().setAuth(auth);

    PaginationData paginationData =
        exportInformation.isPresent() ? exportInformation.get().getPaginationData() : null;
    IdOnlyContainerResource resource =
        exportInformation.isPresent()
            ? (IdOnlyContainerResource) exportInformation.get().getContainerResource()
            : null;
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
        perUserRateLimiter.acquire();
        photoSetList = photosInterface.getNotInSet(PHOTO_PER_PAGE, page);
        RequestContext.getRequestContext().setExtras(ImmutableList.of());
      } else {
        perUserRateLimiter.acquire();
        photoSetList =
            photosetsInterface.getPhotos(
                photoSetId, ImmutableSet.copyOf(EXTRAS), 0, PHOTO_PER_PAGE, page);
      }
    } catch (FlickrException e) {
      return new ExportResult<>(e);
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
      perUserRateLimiter.acquire();
      photoSetList =
          photosetsInterface.getList(
              auth.getUser().getId(), PHOTO_SETS_PER_PAGE, page, PHOTOSET_EXTRAS);
    } catch (FlickrException e) {
      return new ExportResult<>(e);
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
    if (hasMore) {
      newPage = new IntPaginationToken(page + 1);
    }

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
