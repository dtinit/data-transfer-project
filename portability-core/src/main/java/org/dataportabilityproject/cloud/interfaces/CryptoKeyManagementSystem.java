package org.dataportabilityproject.cloud.interfaces;

import java.io.IOException;

/**
 * Stores encryption/decryption keys and manages their usage.
 */
public interface CryptoKeyManagementSystem {
  byte[] decrypt(String cryptoKeyName, byte[] ciphertext) throws IOException;
}
