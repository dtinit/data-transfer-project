package org.dataportabilityproject.gateway.crypto;

import com.google.common.base.Preconditions;
import java.security.PrivateKey;
import java.security.PublicKey;
import javax.crypto.SecretKey;

public class EncrypterFactory {

  /**
   * Creates a {@link EncrypterImpl} with the given {@link SecretKey} for use with "AES"-based symmetric
   * encryption.
   */
  public static Encrypter create(SecretKey key) {
    Preconditions.checkArgument(key.getAlgorithm().equals("AES"));
    return new EncrypterImpl("AES", key);
  }

  /**
   * Creates a {@link EncrypterImpl} with the given {@link PublicKey} for use with "RSA"-based
   * asymmetric encryption.
   */
  public static Encrypter create(PublicKey key) {
    Preconditions.checkArgument(key.getAlgorithm().equals("RSA"));
    return new EncrypterImpl("RSA/ECB/PKCS1Padding", key);
  }

  /**
   * Creates a {@link EncrypterImpl} with the given {@link PrivateKey} for use with "RSA"-based
   * asymmetric encryption.
   */
  public static Encrypter create(PrivateKey key) {
    Preconditions.checkArgument(key.getAlgorithm().equals("RSA"));
    return new EncrypterImpl("RSA/ECB/PKCS1Padding", key);
  }
}
