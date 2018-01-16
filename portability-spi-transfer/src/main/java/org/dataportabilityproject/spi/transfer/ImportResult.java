package org.dataportabilityproject.spi.transfer;

/**
 * The result of an item import operation.
 */
public class ImportResult {
    public static final ImportResult OK = new ImportResult(ResultType.OK);

    /**
     * Result types.
     */
    public enum ResultType {
        /**
         * Indicates a successful import.
         */
        OK,

        /**
         * Indicates an unrecoverable error was raised.
         */
        ERROR,

        /**
         * Indicates a recoverable error was raised.
         */
        RETRY
    }

    private ResultType type;
    private String message;

    /**
     * Ctor used to return error or retry results.
     *
     * @param type the result type
     * @param message the result message, if any
     */
    public ImportResult(ResultType type, String message) {
        this.type = type;
        this.message = message;
    }

    /**
     * Ctor.
     *
     * @param type the result type
     */
    public ImportResult(ResultType type) {
        this.type = type;
    }

    /**
     * Returns the type of result.
     */
    public ResultType getType() {
        return type;
    }

    /**
     * Returns the result message or null if no message is present.
     */
    public String getMessage() {
        return message;
    }
}
