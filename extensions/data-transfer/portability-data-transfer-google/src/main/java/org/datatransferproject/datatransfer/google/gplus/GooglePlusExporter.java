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

package org.datatransferproject.datatransfer.google.gplus;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.services.plus.Plus;
import com.google.api.services.plus.model.Activity;
import com.google.api.services.plus.model.Activity.PlusObject.Attachments;
import com.google.api.services.plus.model.Activity.PlusObject.Attachments.Thumbnails;
import com.google.api.services.plus.model.ActivityFeed;
import com.google.api.services.plus.model.Place;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import org.datatransferproject.datatransfer.google.common.GoogleCredentialFactory;
import org.datatransferproject.datatransfer.google.common.GoogleStaticObjects;
import org.datatransferproject.spi.transfer.provider.ExportResult;
import org.datatransferproject.spi.transfer.provider.ExportResult.ResultType;
import org.datatransferproject.spi.transfer.provider.Exporter;
import org.datatransferproject.spi.transfer.types.ContinuationData;
import org.datatransferproject.types.common.ExportInformation;
import org.datatransferproject.types.common.StringPaginationToken;
import org.datatransferproject.types.common.models.social.SocialActivityActor;
import org.datatransferproject.types.common.models.social.SocialActivityAttachment;
import org.datatransferproject.types.common.models.social.SocialActivityAttachmentType;
import org.datatransferproject.types.common.models.social.SocialActivityContainerResource;
import org.datatransferproject.types.common.models.social.SocialActivityLocation;
import org.datatransferproject.types.common.models.social.SocialActivityModel;
import org.datatransferproject.types.common.models.social.SocialActivityType;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class GooglePlusExporter
    implements Exporter<TokensAndUrlAuthData, SocialActivityContainerResource> {

  private final GoogleCredentialFactory credentialFactory;
  private volatile Plus plusService;

  public GooglePlusExporter(GoogleCredentialFactory credentialFactory) {
    this.credentialFactory = credentialFactory;
    this.plusService = null; // lazily initialized for the given request
  }

  @VisibleForTesting
  GooglePlusExporter(Plus plusService) {
    this.credentialFactory = null; // unused in tests
    this.plusService = plusService;
  }

  @Override
  public ExportResult<SocialActivityContainerResource> export(
      UUID jobId, TokensAndUrlAuthData authData, Optional<ExportInformation> exportInformation) throws IOException {
    Plus plus = getOrCreatePeopleService(authData);

    Plus.Activities.List listActivities = plus.activities().list("me", "public");
    if (exportInformation.isPresent()) {
      StringPaginationToken pageToken = (StringPaginationToken) exportInformation.get().getPaginationData();
      listActivities.setPageToken(pageToken.getToken());
    }

    ActivityFeed activityFeed = listActivities.execute();
    List<Activity> activities = activityFeed.getItems();

    ContinuationData continuationData = null;
    SocialActivityContainerResource results = null;
    if (activities != null && !activities.isEmpty()) {
      List<SocialActivityModel> activityModels = new ArrayList<>();
      Activity.Actor actor = activities.get(0).getActor();
      SocialActivityActor parsedActor =
          new SocialActivityActor(actor.getUrl(), actor.getDisplayName(), actor.getUrl());
      if (!Strings.isNullOrEmpty(activityFeed.getNextPageToken())) {
        continuationData = new ContinuationData(
                new StringPaginationToken(activityFeed.getNextPageToken()));
      }

      for (Activity activity : activities) {
        try {
          activityModels.add(postToActivityModel(activity));
        } catch (RuntimeException e) {
          throw new IOException("Problem exporting: " + activity, e);
        }
      }
      results = new SocialActivityContainerResource(jobId.toString(), parsedActor, activityModels);
    }

    return new ExportResult<>(
            continuationData == null ? ResultType.END : ResultType.CONTINUE,
            results,
            continuationData);

  }

  private SocialActivityModel postToActivityModel(Activity activity) {
    String contentString = activity.getObject().getOriginalContent();
    List<SocialActivityAttachment> activityAttachments = new ArrayList<>();

    switch (activity.getVerb()) {
      case "post":
        for (Attachments attachment : activity.getObject().getAttachments()) {
          if (attachment.getObjectType().equals("article")) {
            activityAttachments.add(
                new SocialActivityAttachment(
                    SocialActivityAttachmentType.LINK,
                    attachment.getUrl(),
                    attachment.getDisplayName(),
                    attachment.getContent()));
          } else if (attachment.getObjectType().equals("photo")) {

            activityAttachments.add(
                new SocialActivityAttachment(
                    SocialActivityAttachmentType.IMAGE,
                    attachment.getFullImage().getUrl(),
                    attachment.getDisplayName(),
                    attachment.getContent()));

          } else if (attachment.getObjectType().equals("album")) {
            // For albums we can't really do the right thing,
            // only the thumbnails and link to the album are provided.
            // And the Google Photos API doesn't surface G+ photos/Albums
            // to enumerate all the images in the album.
            // TODO: see if it is possible to scrape the output from the album to link to
            // all the images.
            for (Thumbnails image : attachment.getThumbnails()) {
              activityAttachments.add(
                  new SocialActivityAttachment(
                      SocialActivityAttachmentType.IMAGE,
                      // This is just a thumbnail image
                      image.getImage().getUrl(),
                      image.getDescription(),
                      // The actual image link isn't to the binary bytes
                      // but instead a hosted page of the image.
                      "Original G+ Image: "
                          + image.getUrl()
                          + " from album: "
                          + attachment.getUrl()));
            }

          } else {
            throw new IllegalArgumentException(
                "Don't know how to export attachment " + attachment.getObjectType());
          }
        }

        return new SocialActivityModel(
            activity.getId(),
            Instant.ofEpochMilli(activity.getPublished().getValue()),
            SocialActivityType.POST,
            activityAttachments,
            null,
            activity.getTitle(),
            contentString,
            activity.getUrl());
      case "checkin":
        Place location = activity.getLocation();
        return new SocialActivityModel(
            activity.getId(),
            Instant.ofEpochMilli(activity.getPublished().getValue()),
            SocialActivityType.CHECKIN,
            null,
            // see https://www.w3.org/TR/activitystreams-vocabulary/#dfn-location

            new SocialActivityLocation(
                location.getDisplayName(),
                location.getPosition().getLongitude(),
                location.getPosition().getLatitude()),
            activity.getPlaceName(),
            contentString,
            null);

      default:
        throw new IllegalArgumentException("Don't know how to export " + activity);
    }
  }

  private Plus getOrCreatePeopleService(TokensAndUrlAuthData authData) {
    return plusService == null ? makePlusService(authData) : plusService;
  }

  private synchronized Plus makePlusService(TokensAndUrlAuthData authData) {
    Credential credential = credentialFactory.createCredential(authData);
    return new Plus.Builder(
            credentialFactory.getHttpTransport(), credentialFactory.getJsonFactory(), credential)
        .setApplicationName(GoogleStaticObjects.APP_NAME)
        .build();
  }
}
