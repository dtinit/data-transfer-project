package org.dataportabilityproject.job;

import static com.google.common.truth.Truth.assertThat;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import org.junit.Test;

public class CrypterImplTest {
  @Test
  public void testRoundTripSecretKey() throws Exception {
    KeyGenerator generator = KeyGenerator.getInstance("AES");
    SecretKey key = generator.generateKey();
    CrypterImpl impl = new CrypterImpl(key);
    String data = "The lazy dog didn't jump over anything";
    assertThat(impl.decrypt(impl.encrypt(data))).isEqualTo(data);
  }
}
