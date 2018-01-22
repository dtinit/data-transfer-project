package org.dataportabilityproject.job;

import com.google.common.base.Charsets;
import com.google.common.io.BaseEncoding;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Handles encryption and decryption with the give {@code key} it is constructed with. */
class CrypterImpl implements Crypter {
  private static final Logger logger = LoggerFactory.getLogger(CrypterImpl.class);
  private final Key key;
  private final String transformation;

  CrypterImpl(String transformation, Key key) {
    this.key = key;
    this.transformation = transformation;
  }

  @Override
  public String encrypt(String data) {
    try {
      Cipher cipher = Cipher.getInstance(transformation);
      cipher.init(Cipher.ENCRYPT_MODE, key);
      byte[] salt = new byte[8];
      SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
      random.nextBytes(salt);
      cipher.update(salt);
      byte[] encrypted = cipher.doFinal(data.getBytes(Charsets.UTF_8));
      return BaseEncoding.base64Url().encode(encrypted);
    } catch (BadPaddingException | IllegalBlockSizeException | InvalidKeyException
        | NoSuchAlgorithmException | NoSuchPaddingException e) {
      logger.error("Exception encrypting data, length: {}", data.length(), e);
      throw new RuntimeException(e);
    }
  }

  @Override
  public String decrypt(String encrypted) {
    try {
      byte[] decoded = BaseEncoding.base64Url().decode(encrypted);
      Cipher cipher = Cipher.getInstance(transformation);
      cipher.init(Cipher.DECRYPT_MODE, key);
      byte[] decrypted = cipher.doFinal(decoded);
      if (decrypted == null || decrypted.length <= 8) {
        throw new RuntimeException("incorrect decrypted text.");
      }
      byte[] data = new byte[decrypted.length - 8];
      System.arraycopy(decrypted, 8, data, 0, data.length);
      return new String(data, Charsets.UTF_8);
    } catch (BadPaddingException | IllegalBlockSizeException | InvalidKeyException
        | NoSuchAlgorithmException | NoSuchPaddingException e) {
      logger.error("Error decrypting data, length: {}", encrypted.length(), e);
      throw new RuntimeException(e);
    }
  }
}
