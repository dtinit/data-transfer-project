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


import org.datatransferproject.spi.transfer.idempotentexecutor.IdempotentImportExecutor;
import org.datatransferproject.spi.transfer.provider.ImportResult;
import org.datatransferproject.spi.transfer.provider.Importer;
import org.datatransferproject.types.common.models.social.SocialActivityContainerResource;
import org.datatransferproject.types.common.models.social.SocialActivityModel;
import org.datatransferproject.types.common.models.social.SocialActivityType;
import org.datatransferproject.types.transfer.auth.CookiesAndUrlAuthData;

import java.io.IOException;
import java.util.UUID;

import static com.google.common.base.Preconditions.checkState;

/**
 * Imports post data to Mastodon.
 * <p> Currently only supports text and not images.
 **/
public class MastodonActivityImport
    implements Importer<CookiesAndUrlAuthData, SocialActivityContainerResource> {

  @Override
  public ImportResult importItem(UUID jobId,
      IdempotentImportExecutor idempotentImportExecutor,
      CookiesAndUrlAuthData authData,
      SocialActivityContainerResource data) throws Exception {
    checkState(authData.getCookies().size() == 1,
        "Exactly 1 cookie expected: %s",
        authData.getCookies());

    MastodonHttpUtilities utilities = new MastodonHttpUtilities(
        authData.getCookies().get(0),
        authData.getUrl());


    for (SocialActivityModel activity : data.getActivities()) {
        if (activity.getType() == SocialActivityType.NOTE) {
          idempotentImportExecutor.executeAndSwallowIOExceptions(
                  activity.getId(),
                  activity.getContent(),
              () -> {
                postNode(activity, utilities, jobId);
                return 1;
              });
        }
      }


    return ImportResult.OK;
  }

  private void postNode(SocialActivityModel activity, MastodonHttpUtilities utilities, UUID jobId) throws IOException {
    utilities.postStatus(
        "Duplicated: " + activity.getContent(),
        jobId + activity.getId());
  }
}
