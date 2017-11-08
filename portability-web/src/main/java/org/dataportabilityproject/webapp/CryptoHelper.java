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

import com.google.common.base.Charsets;
import java.nio.charset.Charset;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import org.dataportabilityproject.job.Crypter;
import org.dataportabilityproject.shared.auth.AuthData;

/**
 * Helper methods to work with encrypting data for the client.
 */
class CryptoHelper {
  private static final Charset DEFAULT_CHARSET = Charsets.UTF_8;
  private final Crypter crypter;

  CryptoHelper(Crypter crypter) {
    this.crypter = crypter;
  }

  /** Encrypts the given {@code authData} and stores it as a cookie. */
  void encryptAndSetCookie(HttpServletResponse response, boolean isExport, AuthData authData) {
    String encrypted = encryptAuthData(authData);
    String cookieKey = isExport ? JsonKeys.EXPORT_AUTH_DATA_COOKIE_KEY : JsonKeys.IMPORT_AUTH_DATA_COOKIE_KEY;
    Cookie authCookie = new Cookie(cookieKey, encrypted);
    LogUtils.log("Set new cookie with key: %s, length: %s", cookieKey, encrypted.length());
    // TODO: reenable. Currently doesn't like the cookie, has a bad value.
    // response.addCookie(authCookie);
  }

  /** Serialize and encrypt the given {@code authData} with the session key. */
  private String encryptAuthData(AuthData authData) {
    String serialized = authData.toString(); // Implement auth data serialization
    return serialized;
    // TODO: reenable
    // byte[] encrypted = crypter.encryptWithSessionKey(serialized);
    // return new String(encrypted, DEFAULT_CHARSET);
  }
}
