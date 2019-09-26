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

package org.datatransferproject.transfer.twitter;

import static java.lang.String.format;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.spi.transfer.provider.ExportResult;
import org.datatransferproject.spi.transfer.provider.ExportResult.ResultType;
import org.datatransferproject.spi.transfer.provider.Exporter;
import org.datatransferproject.spi.transfer.types.ContinuationData;
import org.datatransferproject.types.common.ExportInformation;
import org.datatransferproject.types.common.IntPaginationToken;
import org.datatransferproject.types.common.models.photos.PhotoModel;
import org.datatransferproject.types.common.models.photos.PhotosContainerResource;
import org.datatransferproject.types.transfer.auth.AppCredentials;
import org.datatransferproject.types.transfer.auth.TokenSecretAuthData;
import twitter4j.MediaEntity;
import twitter4j.Paging;
import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;

final class TwitterPhotosExporter
    implements Exporter<TokenSecretAuthData, PhotosContainerResource> {
  private static final int PAGE_SIZE = 5;
  private final AppCredentials appCredentials;
  private final Monitor monitor;

  public TwitterPhotosExporter(AppCredentials appCredentials, Monitor monitor) {
    this.appCredentials = appCredentials;
    this.monitor = monitor;
  }

  @Override
  public ExportResult<PhotosContainerResource> export(
      UUID jobId, TokenSecretAuthData authData, Optional<ExportInformation> exportInformation) {
    Twitter twitterApi = TwitterApiWrapper.getInstance(appCredentials, authData);
    int pageNumber = 1;
    if (exportInformation.isPresent()) {
      IntPaginationToken pageToken =
          (IntPaginationToken) exportInformation.get().getPaginationData();
      if (pageToken != null && pageToken.getStart() > 1) {
        pageNumber = pageToken.getStart();
      }
    }
    Paging paging = new Paging(pageNumber, PAGE_SIZE);
    try {
      String page = "" + pageNumber;
      long id = twitterApi.getId();
      monitor.debug(() -> format("Getting tweets for %s (page %s)", id, page));
      ResponseList<Status> statuses = twitterApi.getUserTimeline(id, paging);
      List<PhotoModel> photos = new ArrayList<>();
      for (Status status : statuses) {
        boolean hasMedia = status.getMediaEntities().length > 0;
        if (hasMedia && !status.isRetweet()) {
          for (MediaEntity mediaEntity : status.getMediaEntities()) {
            photos.add(
                new PhotoModel(
                    "Twitter Photo " + mediaEntity.getId(),
                    mediaEntity.getMediaURL(),
                    status.getText(),
                    null,
                    Long.toString(status.getId()),
                    null,
                    false));
          }
        }
      }
      boolean moreData = statuses.size() == PAGE_SIZE;
      return new ExportResult<>(
          moreData ? ResultType.CONTINUE : ResultType.END,
          new PhotosContainerResource(null, photos),
          moreData ? new ContinuationData(new IntPaginationToken(pageNumber + 1)) : null);
    } catch (TwitterException e) {
      return new ExportResult<>(e);
    }
  }
}
