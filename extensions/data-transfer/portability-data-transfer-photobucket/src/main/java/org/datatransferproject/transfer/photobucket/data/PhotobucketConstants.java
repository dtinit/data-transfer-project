/*
 * Copyright 2021 The Data Transfer Project Authors.
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

package org.datatransferproject.transfer.photobucket.data;

public final class PhotobucketConstants {
  // Keys
  public static final String PB_KEY = "PB_KEY";
  public static final String PB_SECRET = "PB_SECRET";
  public static final String PB_SERVICE_ID = "Photobucket";

  // Titles and prefixes
  public static final String MAIN_ALBUM_TITLE = "Imported media";
  public static final String ALBUM_TITLE_PREFIX = "Copy of ";

  // Clients
  public static final long ACCESS_TOKEN_EXPIRE_TIME_IN_SECONDS = 3600L;
  public static final String GQL_URL = "https://app.photobucket.com/api/graphql";
  public static final String UPLOAD_URL = "https://app.photobucket.com/api/upload_by_url";

  // Headers
  public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
  public static final String AUTHORIZATION_HEADER = "Authorization";

}
