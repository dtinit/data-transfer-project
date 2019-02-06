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
import com.google.api.services.plus.model.ActivityFeed;
import com.google.common.annotations.VisibleForTesting;
import com.ibm.common.activitystreams.Makers;
import org.datatransferproject.datatransfer.google.common.GoogleCredentialFactory;
import org.datatransferproject.datatransfer.google.common.GoogleStaticObjects;
import org.datatransferproject.spi.transfer.provider.ExportResult;
import org.datatransferproject.spi.transfer.provider.ExportResult.ResultType;
import org.datatransferproject.spi.transfer.provider.Exporter;
import org.datatransferproject.spi.transfer.types.ContinuationData;
import org.datatransferproject.types.common.ExportInformation;
import org.datatransferproject.types.common.StringPaginationToken;
import org.datatransferproject.types.common.models.social.SocialActivityContainerResource;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;

import java.io.IOException;
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
      List<com.ibm.common.activitystreams.Activity> parsedActivities = new ArrayList<>();
      continuationData = new ContinuationData(new StringPaginationToken(activityFeed.getNextPageToken()));

      for (Activity activity : activities) {
        parsedActivities.add(postToActivityStream(activity));
      }
      results = new SocialActivityContainerResource(jobId.toString(), parsedActivities, null);
    }

    return new ExportResult<>(
            ResultType.CONTINUE,
            results,
            continuationData);

  }

  private com.ibm.common.activitystreams.Activity postToActivityStream(Activity activity) {
    String contentString = activity.getObject().getOriginalContent();

    return Makers.activity()
            .actor(Makers.object("person")
                    .id("acct:" + activity.getActor().getId())
                    .link("GPlus", activity.getActor().getUrl())
                    .displayName(activity.getActor().getDisplayName()))

            .object(Makers.object("post")
                    .id(activity.getId())
                    .url("GPlus", activity.getUrl())
                    .content(contentString))
            .verb("post")
            .get();
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
