package org.dataportabilityproject.job;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.io.BaseEncoding;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/** Constructs instances of the {@link Crypter} for different key types. */
public class CrypterFactory {

  public static Crypter createCrypterForSecretKey(SecretKey key) {
    Preconditions.checkArgument(key.getAlgorithm().equals("AES"));
    return new CrypterImpl("AES", key);
  }

  public static Crypter createCrypterForPublicKey(PublicKey key) {
    Preconditions.checkArgument(key.getAlgorithm().equals("RSA"));
    return new CrypterImpl("RSA/ECB/PKCS1Padding", key);
  }

  public static Crypter createCrypterForPrivateKey(PrivateKey key) {
    Preconditions.checkArgument(key.getAlgorithm().equals("RSA"));
    return new CrypterImpl("RSA/ECB/PKCS1Padding", key);
  }
}
