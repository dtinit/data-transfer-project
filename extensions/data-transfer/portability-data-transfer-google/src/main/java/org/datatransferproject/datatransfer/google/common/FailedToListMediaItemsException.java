package org.datatransferproject.datatransfer.google.common;

/**
 * FailedToListMediaItemsException is thrown when we try to call PhotosInterface.listMediaItems and are
 * unsuccessful.
 */
public class FailedToListMediaItemsException extends Exception {
    public FailedToListMediaItemsException(String message, Exception cause) {
        super(message, cause);
    }
}
