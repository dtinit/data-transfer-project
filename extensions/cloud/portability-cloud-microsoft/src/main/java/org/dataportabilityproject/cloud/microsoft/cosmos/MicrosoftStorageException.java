package org.dataportabilityproject.cloud.microsoft.cosmos;

/**
 * Raised when unrecoverable errors occur.
 */
public class MicrosoftStorageException extends RuntimeException {

    public MicrosoftStorageException(String message, Throwable cause) {
        super(message, cause);
    }

}
