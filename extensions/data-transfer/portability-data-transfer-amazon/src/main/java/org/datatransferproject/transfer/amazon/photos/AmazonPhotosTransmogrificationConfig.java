/*
 * Copyright 2026 The Data Transfer Project Authors.
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

package org.datatransferproject.transfer.amazon.photos;

import org.datatransferproject.types.common.models.TransmogrificationConfig;

/**
 * Defines the Amazon Photos-specific limits that the DTP transmogrification step applies to
 * incoming data before import — here, the maximum album-name and photo-title lengths. DTP invokes
 * this config (via {@code data.transmogrify(...)}) to normalize/trim source data so it conforms to
 * Amazon Photos constraints.
 */
public class AmazonPhotosTransmogrificationConfig extends TransmogrificationConfig {

  private static final int MAX_ALBUM_NAME_LENGTH = 200;
  private static final int MAX_PHOTO_TITLE_LENGTH = 200;

  @Override
  public int getAlbumNameMaxLength() {
    return MAX_ALBUM_NAME_LENGTH;
  }

  @Override
  public int getPhotoTitleMaxLength() {
    return MAX_PHOTO_TITLE_LENGTH;
  }
}
