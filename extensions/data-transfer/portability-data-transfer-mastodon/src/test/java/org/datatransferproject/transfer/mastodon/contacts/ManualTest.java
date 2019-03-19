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

package org.datatransferproject.transfer.mastodon.contacts;


import com.google.common.collect.ImmutableList;
import org.datatransferproject.spi.transfer.provider.ExportResult;
import org.datatransferproject.test.types.FakeIdempotentImportExecutor;
import org.datatransferproject.transfer.mastodon.social.MastodonActivityExport;
import org.datatransferproject.transfer.mastodon.social.MastodonActivityImport;
import org.datatransferproject.types.common.models.social.SocialActivityContainerResource;
import org.datatransferproject.types.transfer.auth.CookiesAndUrlAuthData;

import java.util.Optional;
import java.util.UUID;

public class ManualTest {
  private static final String ACCESS_TOKEN = "<Put access token here>";
  private static final String HOST_URL = "https://mastodon.social";
  private static final FakeIdempotentImportExecutor EXECUTOR = new FakeIdempotentImportExecutor();


  public static void main(String[] args) throws Exception {
    MastodonActivityExport exporter = new MastodonActivityExport();
    UUID jobId = UUID.randomUUID();
    CookiesAndUrlAuthData authData =
        new CookiesAndUrlAuthData(ImmutableList.of(ACCESS_TOKEN), HOST_URL);

    ExportResult<SocialActivityContainerResource> result = exporter.export(
        jobId,
        authData,
        Optional.empty());

    MastodonActivityImport mastodonActivityImport = new MastodonActivityImport();
    mastodonActivityImport.importItem(jobId, EXECUTOR, authData, result.getExportedData());
  }
}
