/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.dataportabilityproject.webapp;

/** Keys for returned Json data. */
public class JsonKeys {

  private JsonKeys(){} /** no instantiation */

  public static final String ID_COOKIE_KEY = "e_id";
  public static final String EXPORT_AUTH_DATA_COOKIE_KEY = "ead_id";
  public static final String IMPORT_AUTH_DATA_COOKIE_KEY = "iad_id";

  public static final String DATA_TYPE = "dataType";
  public static final String EXPORT = "export";
  public static final String EXPORT_SERVICE = "exportService";
  public static final String EXPORT_SERVICE_AUTH_EXISTS = "exportServiceAuthExists";
  public static final String EXPORT_AUTH_URL = "exportAuthUrl";
  public static final String IMPORT = "import";
  public static final String IMPORT_SERVICE = "importService";
  public static final String IMPORT_SERVICE_AUTH_EXISTS = "importServiceAuthExists";
  public static final String IMPORT_AUTH_URL = "importAuthUrl";
  public static final String TOKEN = "token";

  // These tokens should match the token and header defined in the app.module.ts angular client.
  // see angular docs for more details: https://angular.io/guide/http#security-xsrf-protection
  public static final String XSRF_TOKEN = "XSRF-TOKEN";
  public static final String XSRF_HEADER = "X-XSRF-TOKEN";
}
