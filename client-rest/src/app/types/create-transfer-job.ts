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
    * The data type to transfer.
    */
    dataType: string;

    /**
    * The URL for auth callbacks.
    */
    callbackUrl: string;
}
