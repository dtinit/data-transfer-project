/**
 * Data required to create a transfer job.
 */
export interface CreateTransferJob {
    /**
     * Source (export) service.
     */
    exportService: string;

    /**
     * Target (import) service.
     */
    importService: string;

    /**
     * The URL for export auth callback.
     */
    exportCallbackUrl: string;

    /**
     * The URL for import auth callback.
     */
    importCallbackUrl: string;

    /**
     * The data type to transfer.
     */
    dataType: string;

    /**
     * The encryptionScheme to use.
     */
    encryptionScheme: string;
}
