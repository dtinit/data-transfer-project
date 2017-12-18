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
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;


/** Handles symmetric encryption and decryption with the give {@code key} it is constructed with. */
public class CrypterImpl implements Crypter {
  private static final String ALGORITHM = "AES";
  private final Key key;

  public CrypterImpl(Key key) {
    this.key = key;
  }

  @Override
  public String encrypt(String data) {
    try {
      Cipher cipher = Cipher.getInstance(ALGORITHM);
      cipher.init(Cipher.ENCRYPT_MODE, key);
      byte[] salt = new byte[8];
      SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
      random.nextBytes(salt);
      cipher.update(salt);
      byte[] encrypted = cipher.doFinal(data.getBytes(Charsets.UTF_8));
      return BaseEncoding.base64Url().encode(encrypted);
    } catch (BadPaddingException e) {
      throw new RuntimeException(e);
    } catch (IllegalBlockSizeException e) {
      throw new RuntimeException(e);
    } catch (InvalidKeyException e) {
      throw new RuntimeException(e);
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    } catch (NoSuchPaddingException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public String decrypt(String encrypted) {
    try {
      byte[] decoded = BaseEncoding.base64Url().decode(encrypted);
      Cipher cipher = Cipher.getInstance(ALGORITHM);
      cipher.init(Cipher.DECRYPT_MODE, key);
      byte[] decrypted = cipher.doFinal(decoded);
      if (decrypted == null || decrypted.length <= 8) {
        throw new RuntimeException("incorrect decrypted text.");
      }
      byte[] data = new byte[decrypted.length - 8];
      System.arraycopy(decrypted, 8, data, 0, data.length);
      return new String(data, Charsets.UTF_8);
    } catch (BadPaddingException e) {
      throw new RuntimeException(e);
    } catch (IllegalBlockSizeException e) {
      throw new RuntimeException(e);
    } catch (InvalidKeyException e) {
      throw new RuntimeException(e);
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    } catch (NoSuchPaddingException e) {
      throw new RuntimeException(e);
    }
  }
}
