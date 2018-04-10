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
package org.dataportabilityproject.transfer.instagram.photos;

import java.util.UUID;
import org.dataportabilityproject.spi.transfer.provider.ExportResult;
import org.dataportabilityproject.spi.transfer.provider.Exporter;
import org.dataportabilityproject.spi.transfer.types.ExportInformation;
import org.dataportabilityproject.types.transfer.auth.TokensAndUrlAuthData;
import org.dataportabilityproject.types.transfer.models.photos.PhotosContainerResource;

public class InstagramPhotoExporter implements
    Exporter<TokensAndUrlAuthData, PhotosContainerResource> {

  @Override
  public ExportResult<PhotosContainerResource> export(UUID jobId, TokensAndUrlAuthData authData) {
    return null; // TODO: implement
  }

  @Override
  public ExportResult<PhotosContainerResource> export(UUID jobId, TokensAndUrlAuthData authData,
      ExportInformation exportInformation) {
    return null; // TODO: implement
  }
}
