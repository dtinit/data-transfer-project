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

package org.datatransferproject.datatransfer.google;

import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.datatransferproject.api.launcher.ExtensionContext;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.datatransfer.google.blogger.GoogleBloggerImporter;
import org.datatransferproject.datatransfer.google.common.GoogleCredentialFactory;
import org.datatransferproject.spi.cloud.storage.AppCredentialStore;
import org.datatransferproject.spi.cloud.storage.JobStore;
import org.datatransferproject.spi.transfer.extension.TransferExtension;
import org.datatransferproject.spi.transfer.provider.Exporter;
import org.datatransferproject.spi.transfer.provider.Importer;
import org.datatransferproject.types.transfer.auth.AppCredentials;

import java.io.IOException;

/*
 * BloggerTransferExtension allows for importers and exporters (not yet implemented) of
 * Google Blogger data.
 */
// This needs to be separated out from Google, as there are multiple services that want to
// handle SOCIAL-POSTS.
public class BloggerTransferExtension implements TransferExtension {
  private static final String SERVICE_ID = "GoogleBlogger";

  // TODO: centralized place, or enum type for these
  private static final ImmutableList<String> SUPPORTED_SERVICES =
      ImmutableList.of("SOCIAL-POSTS");
  private ImmutableMap<String, Importer> importerMap;
  private ImmutableMap<String, Exporter> exporterMap;
  private boolean initialized = false;

  @Override
  public String getServiceId() {
    return SERVICE_ID;
  }

  @Override
  public Exporter<?, ?> getExporter(String transferDataType) {
    Preconditions.checkArgument(initialized);
    Preconditions.checkArgument(SUPPORTED_SERVICES.contains(transferDataType));
    return exporterMap.get(transferDataType);
  }

  @Override
  public Importer<?, ?> getImporter(String transferDataType) {
    Preconditions.checkArgument(initialized);
    Preconditions.checkArgument(SUPPORTED_SERVICES.contains(transferDataType));
    return importerMap.get(transferDataType);
  }

  @Override
  public void initialize(ExtensionContext context) {
    // Note: initialize could be called twice in an account migration scenario where we import and
    // export to the same service provider. So just return rather than throwing if called multiple
    // times.
    if (initialized) return;

    AppCredentials appCredentials;
    try {
      appCredentials =
          context
              .getService(AppCredentialStore.class)
              .getAppCredentials("GOOGLEBLOGGER_KEY", "GOOGLEBLOGGER_SECRET");
    } catch (IOException e) {
      Monitor monitor = context.getMonitor();
      monitor.info(
          () -> "Unable to retrieve Google AppCredentials. "
              + "Did you set GOOGLEBLOGGER_KEY and GOOGLEBLOGGER_SECRET?");
      return;
    }

    HttpTransport httpTransport = context.getService(HttpTransport.class);
    JobStore jobStore = context.getService(JobStore.class);
    JsonFactory jsonFactory = context.getService(JsonFactory.class);

    // Create the GoogleCredentialFactory with the given {@link AppCredentials}.
    GoogleCredentialFactory credentialFactory =
        new GoogleCredentialFactory(httpTransport, jsonFactory, appCredentials);

    ImmutableMap.Builder<String, Importer> importerBuilder = ImmutableMap.builder();

    importerBuilder.put("SOCIAL-POSTS", new GoogleBloggerImporter(credentialFactory, jobStore));

    importerMap = importerBuilder.build();

    ImmutableMap.Builder<String, Exporter> exporterBuilder = ImmutableMap.builder();

    exporterMap = exporterBuilder.build();

    initialized = true;
  }
}