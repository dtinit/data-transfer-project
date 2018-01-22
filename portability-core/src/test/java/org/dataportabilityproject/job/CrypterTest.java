package org.dataportabilityproject.job;

import static com.google.common.truth.Truth.assertThat;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import org.junit.Test;

public class CrypterTest {
  @Test
  public void testRoundTripSecretKey() throws Exception {
    KeyGenerator generator = KeyGenerator.getInstance("AES");
    SecretKey key = generator.generateKey();
    Crypter crypter = CrypterFactory.create(key);
    String data = "The lazy dog didn't jump over anything except \u2614, and a \u26F5";
    assertThat(crypter.decrypt(crypter.encrypt(data))).isEqualTo(data);
  }

  @Test
  public void testRoundTripPublicPrivateKey() throws Exception {
    KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
    KeyPair keyPair = generator.genKeyPair();
    PublicKey publicKey = keyPair.getPublic();
    PrivateKey privateKey = keyPair.getPrivate();
    Crypter encrypter = CrypterFactory.create(publicKey);
    Crypter decrypter = CrypterFactory.create(privateKey);
    String data = "The lazy dog didn't jump over anything except \u2614, and a \u26F5";
    assertThat(decrypter.decrypt(encrypter.encrypt(data))).isEqualTo(data);
  }
}
