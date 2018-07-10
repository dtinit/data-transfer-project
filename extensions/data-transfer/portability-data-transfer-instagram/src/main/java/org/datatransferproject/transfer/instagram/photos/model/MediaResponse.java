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

package org.datatransferproject.transfer.instagram.photos.model;

import java.util.List;

/** DataModel for the result of a media query in the Instagram API. Instantiated by JSON mapping. */
public final class MediaResponse {

  private Meta meta;

  private List<MediaFeedData> data;

  public Meta getMeta() {
    return meta;
  }

  public List<MediaFeedData> getData() {
    return data;
  }
}
