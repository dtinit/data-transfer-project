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
    response.addCookie(authCookie);
  }

  /** Serialize and encrypt the given {@code authData} with the session key. */
  private String encryptAuthData(AuthData authData) {
    String serialized = authData.toString(); // Implement auth data serialization
    byte[] encrypted = crypter.encryptWithSessionKey(serialized);
    return new String(encrypted, DEFAULT_CHARSET);
  }
}
