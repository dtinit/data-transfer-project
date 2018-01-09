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
import static org.apache.axis.transport.http.HTTPConstants.HEADER_SET_COOKIE2;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.sun.net.httpserver.Headers;
import java.security.PublicKey;
import javax.crypto.SecretKey;
import java.net.HttpCookie;
import org.dataportabilityproject.job.Crypter;
import org.dataportabilityproject.job.CrypterImpl;
import org.dataportabilityproject.job.JobDao;
import org.dataportabilityproject.job.PortabilityJob;
import org.dataportabilityproject.job.SessionKeyGenerator;
import org.dataportabilityproject.shared.auth.AuthData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper methods utlized for encrypting data for the client.
 */
class CryptoHelper {
  private final Logger logger = LoggerFactory.getLogger(CryptoHelper.class);

  private static final Gson GSON = new Gson();
  private final JobDao jobDao;

  CryptoHelper(JobDao jobDao) {
    this.jobDao = jobDao;
  }

  /** Encrypts the given {@code authData} and stores it as a cookie in the provided headers. */
  void encryptAndSetCookie(Headers headers, String jobId, boolean isExport, AuthData authData){
    SecretKey sessionKey = getSessionKey(jobId);
    String encrypted = encryptAuthData(sessionKey, authData);
    String cookieKey = isExport ? JsonKeys.EXPORT_AUTH_DATA_COOKIE_KEY : JsonKeys.IMPORT_AUTH_DATA_COOKIE_KEY;
    HttpCookie cookie = new HttpCookie(cookieKey, encrypted);
    logger.debug("Set new cookie with key: {}, length: {} for job: {}",
        cookieKey, encrypted.length(), jobId);
    headers.add(HEADER_SET_COOKIE, cookie.toString() + PortabilityApiUtils.COOKIE_ATTRIBUTES);
  }

  private SecretKey getSessionKey(String jobId) {
    PortabilityJob job = jobDao.lookupJobPendingAuthData(jobId);
    String encodedSessionKey = job.sessionKey();
    Preconditions.checkState(!Strings.isNullOrEmpty(encodedSessionKey), "Session key should not be null");
    return SessionKeyGenerator.parse(encodedSessionKey);
  }

  /** Serialize and encrypt the given {@code authData} with the session key. */
  public static String encryptAuthData(PublicKey key, String sessionEncryptedAuthData) {
    Crypter crypter = new CrypterImpl(key);
    return crypter.encrypt(sessionEncryptedAuthData);
  }

  /** Serialize and encrypt the given {@code authData} with the session key. */
  private static String encryptAuthData(SecretKey key, AuthData authData) {
    Crypter crypter = new CrypterImpl(key);
    String serialized = serialize(authData);
    return crypter.encrypt(serialized);
  }

  private static String serialize(AuthData authData) {
    return GSON.toJson(authData, AuthData.class);
  }
}
