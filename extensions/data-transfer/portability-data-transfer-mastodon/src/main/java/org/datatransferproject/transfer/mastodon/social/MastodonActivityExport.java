/*
 * Copyright 2019 The Data Transfer Project Authors.
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

package org.datatransferproject.transfer.mastodon.social;

import com.google.common.base.Strings;
import org.datatransferproject.spi.transfer.provider.ExportResult;
import org.datatransferproject.spi.transfer.provider.ExportResult.ResultType;
import org.datatransferproject.spi.transfer.provider.Exporter;
import org.datatransferproject.spi.transfer.types.ContinuationData;
import org.datatransferproject.transfer.mastodon.model.Account;
import org.datatransferproject.transfer.mastodon.model.Status;
import org.datatransferproject.types.common.ExportInformation;
import org.datatransferproject.types.common.StringPaginationToken;
import org.datatransferproject.types.common.models.social.SocialActivityActor;
import org.datatransferproject.types.common.models.social.SocialActivityContainerResource;
import org.datatransferproject.types.common.models.social.SocialActivityModel;
import org.datatransferproject.types.common.models.social.SocialActivityType;
import org.datatransferproject.types.transfer.auth.CookiesAndUrlAuthData;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkState;

/**
 * Exports post data from Mastodon.
 * <p> Currently only supports text and not images.
 **/
public class MastodonActivityExport implements
        Exporter<CookiesAndUrlAuthData, SocialActivityContainerResource> {
  private static final Pattern RAW_CONTENT_PATTERN = Pattern.compile("<p>(.*)</p>");

  @Override
  public ExportResult<SocialActivityContainerResource> export(UUID jobId,
                                                              CookiesAndUrlAuthData authData, Optional<ExportInformation> exportInformation)
          throws Exception {
    checkState(authData.getCookies().size() == 1,
            "Exactly 1 cookie expected: %s",
            authData.getCookies());

    String maxId = null;
    if (exportInformation.isPresent()) {
      StringPaginationToken pageData =
          (StringPaginationToken) exportInformation.get().getPaginationData();
      if (!Strings.isNullOrEmpty(pageData.getToken())) {
        maxId = pageData.getToken();
      }
    }

    MastodonHttpUtilities utilities = new MastodonHttpUtilities(
            authData.getCookies().get(0),
            authData.getUrl());

    Account account = utilities.getAccount();

    Status[] statuses = utilities.getStatuses(maxId);
    List<SocialActivityModel> activityList = new ArrayList<>(statuses.length);

    SocialActivityActor actor =
        new SocialActivityActor(
            "acct:" + account.getUsername() + "@" + utilities.getHostName(),
            account.getDisplayName(),
            account.getUrl());


    ContinuationData continuationData = null;
    if (statuses.length > 0) {
      String lastId = null;
      for (Status status : statuses) {
        activityList.add(statusToActivity(account, status, utilities));
        lastId = status.getId();
      }
      continuationData = new ContinuationData(new StringPaginationToken(lastId));
    }

    return new ExportResult<>(
        continuationData == null ? ResultType.END : ResultType.CONTINUE,
        new SocialActivityContainerResource(account.getId() + maxId, actor, activityList),
        continuationData);
  }

  private SocialActivityModel statusToActivity(
      Account account, Status status, MastodonHttpUtilities utilities) {
    String contentString = status.getContent();
    Matcher matcher = RAW_CONTENT_PATTERN.matcher(contentString);
    if (matcher.matches()) {
      contentString = matcher.group(1);
    }

    return new SocialActivityModel(
        status.getUri(),
        status.getCreatedAt(),
        SocialActivityType.NOTE,
        null,
        null,
        null,
        contentString,
        status.getUrl());
  }
}
