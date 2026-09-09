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
 * Amazon Photos size limits applied to a {@code MediaContainerResource} during the DTP
 * transmogrification step: the maximum album-name, photo-title and video-title lengths that source
 * data is trimmed to before import.
 */
public class AmazonMediaTransmogrificationConfig extends TransmogrificationConfig {

  private static final int MAX_ALBUM_NAME_LENGTH = 200;
  private static final int MAX_PHOTO_TITLE_LENGTH = 200;
  private static final int MAX_VIDEO_TITLE_LENGTH = 200;

  @Override
  public int getAlbumNameMaxLength() {
    return MAX_ALBUM_NAME_LENGTH;
  }

  @Override
  public int getPhotoTitleMaxLength() {
    return MAX_PHOTO_TITLE_LENGTH;
  }

  @Override
  public int getVideoTitleMaxLength() {
    return MAX_VIDEO_TITLE_LENGTH;
  }
}
