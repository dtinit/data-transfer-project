package org.datatransferproject.transfer.koofr.exceptions;

import java.io.IOException;

public class KoofrClientIOException extends IOException {

    private final int code;

    public KoofrClientIOException(int code,
                                  String message,
                                  String body) {
        super(String.format("Got error code: %s message: %s body: %s", code, message, body));

        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
