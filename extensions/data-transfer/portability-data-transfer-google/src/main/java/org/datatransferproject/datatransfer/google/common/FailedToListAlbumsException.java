package org.datatransferproject.datatransfer.google.common;

/**
 * FailedToListAlbumsException is thrown when we try to call PhotosInterface.listAlbums and are
 * unsuccessful.
 */
public class FailedToListAlbumsException extends Exception {
    public FailedToListAlbumsException(String message, Exception cause) {
        super(message, cause);
    }
}
