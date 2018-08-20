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

package org.datatransferproject.datatransfer.flickr;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import java.util.Set;
import org.datatransferproject.api.launcher.ExtensionContext;
import org.datatransferproject.datatransfer.flickr.photos.FlickrPhotosExporter;
import org.datatransferproject.datatransfer.flickr.photos.FlickrPhotosImporter;
import org.datatransferproject.spi.cloud.storage.AppCredentialStore;
import org.datatransferproject.spi.cloud.storage.JobStore;
import org.datatransferproject.spi.transfer.extension.TransferExtension;
import org.datatransferproject.spi.transfer.provider.Exporter;
import org.datatransferproject.spi.transfer.provider.Importer;
import org.datatransferproject.types.transfer.auth.AppCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlickrTransferExtension implements TransferExtension {
  private static final String SERVICE_ID = "flickr";
  private static final String FLICKR_KEY = "FLICKR_KEY";
  private static final String FLICKER_SECRET = "FLICKR_SECRET";

  private final Logger logger = LoggerFactory.getLogger(FlickrTransferExtension.class);
  private final Set<String> supportedServices = ImmutableSet.of("PHOTOS");

  private Importer importer;
  private Exporter exporter;
  private JobStore jobStore;
  private boolean initialized = false;
  private AppCredentials appCredentials;

  @Override
  public String getServiceId() {
    return SERVICE_ID;
  }

  @Override
  public Exporter<?, ?> getExporter(String transferDataType) {
    Preconditions.checkArgument(initialized);
    Preconditions.checkArgument(supportedServices.contains(transferDataType));
    return exporter;
  }

  @Override
  public Importer<?, ?> getImporter(String transferDataType) {
    Preconditions.checkArgument(initialized);
    Preconditions.checkArgument(supportedServices.contains(transferDataType));
    return importer;
  }

  @Override
  public void initialize(ExtensionContext context) {
    if (initialized) return;
    jobStore = context.getService(JobStore.class);

    try {
      appCredentials =
          context
              .getService(AppCredentialStore.class)
              .getAppCredentials(FLICKR_KEY, FLICKER_SECRET);
    } catch (Exception e) {
      logger.warn(
          "Problem getting AppCredentials: {} Did you set {} and {}?",
          e.getMessage(),
          FLICKR_KEY,
          FLICKER_SECRET);
      initialized = false;
      return;
    }

    importer = new FlickrPhotosImporter(appCredentials, jobStore);
    exporter = new FlickrPhotosExporter(appCredentials);
    initialized = true;
  }
}
