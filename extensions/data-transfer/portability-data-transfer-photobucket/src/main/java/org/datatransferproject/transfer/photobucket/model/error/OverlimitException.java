package org.datatransferproject.transfer.photobucket.model.error;

public class OverlimitException extends Exception {
    public OverlimitException() {
        super("User reached his limits, unable to proceed with data import.");
    }
}
