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
  public static final String ENVIRONMENT = "https://app.photobucket.com";
  // Keys
  public static final String PHOTOBUCKET_KEY = "PHOTOBUCKET_KEY";
  public static final String PHOTOBUCKET_SECRET = "PHOTOBUCKET_SECRET";
  public static final String PB_SERVICE_ID = "Photobucket";

  // Titles and prefixes
  public static final String MAIN_ALBUM_TITLE = "Imported media";
  public static final String ALBUM_TITLE_PREFIX = "Copy of ";

  // Clients
  public static final long ACCESS_TOKEN_EXPIRE_TIME_IN_SECONDS = 60 * 60 * 24 * 7L;
  public static final String GQL_URL = ENVIRONMENT + "/api/graphql";
  public static final String UPLOAD_BY_URL_URL = ENVIRONMENT + "/api/upload_by_url";
  public static final String UPLOAD_URL = ENVIRONMENT + "/api/uploadMobile";
  public static final String USER_STATS_URL = ENVIRONMENT + "/me";

  // Headers
  public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
  public static final String AUTHORIZATION_HEADER = "Authorization";
  public static final String REFERER_HEADER = "Referer";
  public static final String ORIGIN_HEADER = "Origin";
  public static final String REFERER_HEADER_VALUE = ENVIRONMENT;
  public static final String ORIGIN_HEADER_VALUE = REFERER_HEADER_VALUE;

}
