package org.dataportabilityproject.gateway.crypto;

import com.google.common.base.Preconditions;
import java.security.PrivateKey;
import java.security.PublicKey;
import javax.crypto.SecretKey;

public class DecrypterFactory {

  /**
   * Creates a {@link DecrypterImpl} with the given {@link SecretKey} for use with "AES"-based symmetric
   * encryption.
   */
  public static Decrypter create(SecretKey key) {
    Preconditions.checkArgument(key.getAlgorithm().equals("AES"));
    return new DecrypterImpl("AES", key);
  }

  /**
   * Creates a {@link DecrypterImpl} with the given {@link PublicKey} for use with "RSA"-based
   * asymmetric encryption.
   */
  public static Decrypter create(PublicKey key) {
    Preconditions.checkArgument(key.getAlgorithm().equals("RSA"));
    return new DecrypterImpl("RSA/ECB/PKCS1Padding", key);
  }

  /**
   * Creates a {@link DecrypterImpl} with the given {@link PrivateKey} for use with "RSA"-based
   * asymmetric encryption.
   */
  public static Decrypter create(PrivateKey key) {
    Preconditions.checkArgument(key.getAlgorithm().equals("RSA"));
    return new DecrypterImpl("RSA/ECB/PKCS1Padding", key);
  }
}
