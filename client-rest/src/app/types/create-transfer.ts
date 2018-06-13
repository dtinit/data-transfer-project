/**
 * Data required to create a transfer operation.
 */
export interface CreateTransfer {

    /**
     * Source (export) service.
     */
    source: string;

    /**
     * Target (import) service.
     */
    destination: string;

    /**
     * The data type to transfer.
     */
    transferDataType: string;
}