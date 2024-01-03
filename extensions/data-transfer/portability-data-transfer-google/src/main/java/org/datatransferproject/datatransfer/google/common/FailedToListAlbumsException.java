package org.datatransferproject.datatransfer.google.common;

/**
 * FailedToListAlbumsException is thrown when we try to call PhotosInterface.listAlbums with retrying, and are
 * unsuccessful.
 */
public class FailedToListAlbumsException extends Exception {
    public FailedToListAlbumsException(String message, Exception cause) {
        super(message, cause);
    }
}
