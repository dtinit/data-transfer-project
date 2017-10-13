package org.dataportabilityproject.job;

/** Provides functions related to encryption. */
public interface Crypter {

  /**
   * Encrypt the given {@code data} with the session key.
   */
  default public byte[] encryptWithSessionKey(String data) {
    // TODO: Implementation generates and stores session key
    throw new UnsupportedOperationException("To be implemented");
  }

  /**
   * Decrypts the given {@code encrypted} bytes with the session key
   */
  default public String decryptWithSessionKey(byte[] encrypted) {
    // TODO: Impelementation looks up session key and decrypts bytes
    throw new UnsupportedOperationException("To be implemented");
  }
}
