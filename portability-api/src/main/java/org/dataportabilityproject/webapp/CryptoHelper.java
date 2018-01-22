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

import static org.apache.axis.transport.http.HTTPConstants.HEADER_SET_COOKIE;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.inject.Inject;
import com.sun.net.httpserver.Headers;
import java.net.HttpCookie;
import javax.crypto.SecretKey;
import org.dataportabilityproject.job.Crypter;
import org.dataportabilityproject.job.CrypterFactory;
import org.dataportabilityproject.job.JobDao;
import org.dataportabilityproject.job.PortabilityJob;
import org.dataportabilityproject.job.SecretKeyGenerator;
import org.dataportabilityproject.shared.ServiceMode;
import org.dataportabilityproject.shared.auth.AuthData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper methods utilized for encrypting and decrypting data for the client.
 */
class CryptoHelper {
  private static final Logger logger = LoggerFactory.getLogger(CryptoHelper.class);
  private static final Gson GSON = new Gson();
  private final JobDao jobDao;

  @Inject
  CryptoHelper(JobDao jobDao) {
    this.jobDao = jobDao;
  }

   /**
   * Serialize and encrypt the given {@code authData} with the given {@link SecretKey}.
   */
  private static String encrypt(SecretKey key, AuthData authData) {
    Crypter crypter = CrypterFactory.create(key);
    String serialized = serialize(authData);
    return crypter.encrypt(serialized);
  }

  private static String serialize(AuthData authData) {
    return GSON.toJson(authData, AuthData.class);
  }

  /**
   * Encrypts the given {@code authData} with the session-based {@link SecretKey} and stores it as a
   * cookie in the provided headers.
   */
  void encryptAndSetCookie(Headers headers, String jobId, ServiceMode serviceMode,
      AuthData authData) {
    SecretKey sessionKey = getSessionKey(jobId);
    String encrypted = encrypt(sessionKey, authData);
    String cookieKey = (serviceMode == ServiceMode.EXPORT) ? JsonKeys.EXPORT_AUTH_DATA_COOKIE_KEY
        : JsonKeys.IMPORT_AUTH_DATA_COOKIE_KEY;
    HttpCookie cookie = new HttpCookie(cookieKey, encrypted);
    logger.debug("Set new cookie with key: {}, length: {} for job: {}",
        cookieKey, encrypted.length(), jobId);
    headers.add(HEADER_SET_COOKIE, cookie.toString() + PortabilityApiUtils.COOKIE_ATTRIBUTES);
  }

  private SecretKey getSessionKey(String jobId) {
    PortabilityJob job = jobDao.lookupJobPendingAuthData(jobId);
    String encodedSessionKey = job.sessionKey();
    Preconditions
        .checkState(!Strings.isNullOrEmpty(encodedSessionKey), "Session key should not be null");
    return SecretKeyGenerator.parse(encodedSessionKey);
  }
}
